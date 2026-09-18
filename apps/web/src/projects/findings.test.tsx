import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, expect, it, vi } from 'vitest';
import { ApiError, createAnalysis, getAnalysis, listFindingActions, listFindings, listShots, type AnalysisRun, type Finding, type Shot } from '@sceneproof/api-client';
import { Providers } from '../providers';
import { FindingsWorkspace } from './FindingsWorkspace';
import { mapEvidence } from './EvidenceComparison';

vi.mock('@sceneproof/api-client', async importOriginal => ({ ...await importOriginal<typeof import('@sceneproof/api-client')>(), listShots: vi.fn(), listFindings: vi.fn(), listFindingActions: vi.fn(), getAnalysis: vi.fn(), createAnalysis: vi.fn() }));

const shots: Shot[] = [0, 1, 2].map(position => ({ id: `shot-${position}`, position, name: `shot-${position}.png`, kind: 'IMAGE', status: 'READY', failureCode: null, durationMs: null, frames: [{ id: `frame-${position}`, position: 0, timestampMs: null, width: 160, height: 90, url: `/frame-${position}.png` }] }));
const finding: Finding = { id: 'finding-1', analysisRunId: 'run-1', category: 'PROP', severity: 'HIGH', confidence: 0.9, title: 'Color drift', summary: 'The square changes color.', expectedState: 'Red square', observedState: 'Blue square', explanation: 'The rule requires a red square.', affectedShotIds: ['shot-0', 'shot-1'], relevantFrameIds: ['frame-1', 'frame-0'], relevantReferenceIds: [], suggestedCorrectionPrompt: 'Keep the square red.', status: 'OPEN' };
const run: AnalysisRun = { id: 'run-1', kind: 'SEQUENCE', projectId: 'project', requestId: 'request', status: 'SUCCEEDED', provider: 'OPENAI', model: 'gpt-6-astra', startedAt: '2026-09-12T16:00:00Z', completedAt: '2026-09-12T16:00:01Z', failureCode: null, failureMessage: null, shotCount: 2, frameCount: 2, projectSummary: 'Reviewed', warnings: [], usage: null, providerResponseId: null, providerRequestId: null };

function Location() { return <output aria-label="Current URL">{useLocation().search}</output>; }
function open(query = '') { return render(<Providers><MemoryRouter initialEntries={[`/projects/project${query}`]}><FindingsWorkspace projectId="project" /><Location /></MemoryRouter></Providers>); }

it('keeps contextual roadmap help collapsed and clearly separate from current capabilities', async () => {
  const user = userEvent.setup(); open();
  const help = screen.getByText('Why this exists');
  expect(help.closest('details')).not.toHaveAttribute('open');
  expect(screen.getByText(/Planned: Source Monitor/)).not.toBeVisible();
  await user.click(help);
  expect(screen.getByText(/connects a specific continuity concern/)).toBeVisible();
  expect(screen.getByText(/Planned: Source Monitor/)).toHaveTextContent('not available in this MVP');
  expect(screen.getByText(/Planned: Fix Assist/)).toHaveTextContent('export and generation are not available');
  expect(screen.queryByRole('button', { name: /Fix Assist|Source Monitor|Analyze Selection/ })).not.toBeInTheDocument();
});
beforeEach(() => {
  vi.mocked(createAnalysis).mockReset().mockResolvedValue(run);
  vi.mocked(listFindingActions).mockResolvedValue([]);
  vi.mocked(listShots).mockResolvedValue(shots);
  vi.mocked(listFindings).mockResolvedValue([finding]);
  vi.mocked(getAnalysis).mockResolvedValue(run);
  Element.prototype.scrollIntoView = vi.fn();
});

it('disables analysis until at least one successfully imported shot is available', async () => {
  vi.mocked(listShots).mockResolvedValue([]);
  const first = open();
  expect(screen.getByRole('button', { name: 'Run continuity analysis' })).toBeDisabled();
  expect(await screen.findByText(/Import at least one shot successfully/)).toBeVisible();
  fireEvent.click(screen.getByRole('button', { name: 'Run continuity analysis' }));
  expect(createAnalysis).not.toHaveBeenCalled();
  first.unmount();
  vi.mocked(listShots).mockResolvedValue([{ ...shots[0]!, status: 'FAILED', frames: [] }]);
  open();
  expect(await screen.findByText(/Import at least one shot successfully/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'Run continuity analysis' })).toBeDisabled();
});

it('starts once, announces pending work and navigates to returned saved findings clearing stale selection', async () => {
  let finish!: (value: AnalysisRun) => void;
  vi.mocked(createAnalysis).mockImplementationOnce(() => new Promise(resolve => { finish = resolve; }));
  open('?analysisId=old-run&finding=finding-1&findingsPage=2&tab=findings');
  const button = screen.getByRole('button', { name: 'Run continuity analysis' });
  await waitFor(() => expect(button).toBeEnabled());
  fireEvent.click(button);
  fireEvent.click(button);
  expect(createAnalysis).toHaveBeenCalledTimes(1);
  expect(createAnalysis).toHaveBeenCalledWith('project', expect.stringMatching(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i));
  expect(button).toBeDisabled();
  expect(button).toHaveAttribute('aria-busy', 'true');
  expect(screen.getAllByRole('status').some(status => status.textContent === 'Analyzing continuity…')).toBe(true);
  finish({ ...run, id: 'returned-run' });
  await waitFor(() => expect(screen.getByLabelText('Current URL')).toHaveTextContent('?analysisId=returned-run&tab=findings'));
  await waitFor(() => expect(listFindings).toHaveBeenCalledWith('project', 'returned-run', 0, expect.any(AbortSignal)));
  expect(screen.queryByRole('heading', { name: 'Finding evidence' })).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Run continuity analysis' })).toBeEnabled();
});

it('reuses the request UUID after transport failure, never retries automatically, and generates a new UUID only after a returned terminal run', async () => {
  vi.mocked(createAnalysis).mockRejectedValueOnce(new TypeError('Network connection lost'));
  const user = userEvent.setup();
  open();
  const button = screen.getByRole('button', { name: 'Run continuity analysis' });
  await waitFor(() => expect(button).toBeEnabled());
  await user.click(button);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network connection lost');
  expect(createAnalysis).toHaveBeenCalledTimes(1);
  const originalRequestId = vi.mocked(createAnalysis).mock.calls[0]![1];
  await user.click(screen.getByRole('button', { name: 'Retry continuity analysis' }));
  await waitFor(() => expect(screen.getByLabelText('Current URL')).toHaveTextContent('analysisId=run-1'));
  expect(createAnalysis).toHaveBeenNthCalledWith(2, 'project', originalRequestId);
  expect(screen.queryByText('Network connection lost')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Run continuity analysis' }));
  expect(createAnalysis).toHaveBeenCalledTimes(3);
  expect(vi.mocked(createAnalysis).mock.calls[2]![1]).not.toBe(originalRequestId);
});

it.each([
  [429, 'New paid runs are disabled.'],
  [429, 'The paid-run hourly limit has been reached.'],
  [502, 'The provider rejected this analysis request.'],
])('shows HTTP %s backend errors and keeps the request UUID on explicit retry: %s', async (status, detail) => {
  vi.mocked(createAnalysis).mockRejectedValue(new ApiError(status, { type: 'about:blank', title: 'Analysis rejected', status, detail }));
  const user = userEvent.setup();
  open();
  const button = screen.getByRole('button', { name: 'Run continuity analysis' });
  await waitFor(() => expect(button).toBeEnabled());
  await user.click(button);
  expect(await screen.findByRole('alert')).toHaveTextContent(`HTTP ${status}: ${detail}`);
  expect(createAnalysis).toHaveBeenCalledTimes(1);
  await user.click(screen.getByRole('button', { name: 'Retry continuity analysis' }));
  expect(createAnalysis).toHaveBeenCalledTimes(2);
  expect(vi.mocked(createAnalysis).mock.calls[1]).toEqual(vi.mocked(createAnalysis).mock.calls[0]);
});

it('loads saved findings, selects real evidence and highlights only affected shots', async () => {
  const user = userEvent.setup();
  open();
  expect(screen.getByText('Loading findings…')).toBeVisible();
  await user.click(await screen.findByRole('button', { name: /Color drift/ }));
  expect(screen.getByRole('heading', { name: 'Finding evidence' })).toBeVisible();
  expect(screen.getByRole('img', { name: 'Evidence: shot-0.png, frame 1' })).toHaveAttribute('src', '/frame-0.png');
  expect(screen.getByRole('img', { name: 'Evidence: shot-1.png, frame 1' })).toHaveAttribute('src', '/frame-1.png');
  expect(document.getElementById('shot-shot-0')).toHaveClass('shot-affected');
  expect(document.getElementById('shot-shot-1')).toHaveClass('shot-affected');
  expect(document.getElementById('shot-shot-2')).not.toHaveClass('shot-affected');
  expect(screen.getByText('Red square')).toBeVisible();
  expect(screen.getByText('Blue square')).toBeVisible();
  expect(screen.getByText(finding.explanation)).toBeVisible();
  expect(screen.getByText('Model confidence: 90%')).toBeVisible();
  expect(screen.getByText(finding.suggestedCorrectionPrompt)).toBeVisible();
  expect(screen.getByLabelText('Current URL')).toHaveTextContent('finding=finding-1');
  await user.click(screen.getByRole('button', { name: 'Shot 2 · shot-1.png' }));
  expect(screen.getByRole('button', { name: 'Select shot-1.png frame 1' })).toHaveFocus();
  await user.click(screen.getByRole('button', { name: 'Select shot-2.png frame 1' }));
  expect(screen.getByRole('img', { name: 'Frame from shot-2.png' })).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Back to finding evidence' }));
  expect(screen.getByRole('heading', { name: 'Finding evidence' })).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Reload saved findings' }));
  await waitFor(() => expect(listFindings).toHaveBeenCalledTimes(3));
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('restores analysis, page and finding from a reloaded URL without provider work', async () => {
  open('?analysisId=run-1&findingsPage=2&finding=finding-1');
  expect(await screen.findByRole('heading', { name: 'Finding evidence' })).toBeVisible();
  expect(listFindings).toHaveBeenCalledWith('project', 'run-1', 2, expect.any(AbortSignal));
  expect(getAnalysis).toHaveBeenCalledWith('project', 'run-1', expect.any(AbortSignal));
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('shows honest empty and successful zero-finding states', async () => {
  vi.mocked(listFindings).mockResolvedValue([]);
  const first = open();
  expect(await screen.findByText('No saved findings here.')).toBeVisible();
  expect(screen.getByText(/An empty list alone cannot tell/)).toBeVisible();
  first.unmount();
  open('?analysisId=run-1');
  expect(await screen.findByText('No continuity issues were found.')).toBeVisible();
  expect(screen.getByText('Analysis complete.')).toBeVisible();
  expect(screen.queryByText('No saved findings here.')).not.toBeInTheDocument();
});

it('guides a new project into its first analysis and distinguishes a successful zero-finding POST', async () => {
  vi.mocked(listShots).mockResolvedValue([]);
  vi.mocked(listFindings).mockResolvedValue([]);
  const first = open();
  expect(await screen.findByText('No continuity analysis has been run yet.')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Run continuity analysis' })).toBeDisabled();
  first.unmount();
  vi.mocked(listShots).mockResolvedValue(shots);
  const user = userEvent.setup();
  open();
  const button = screen.getByRole('button', { name: 'Run continuity analysis' });
  await waitFor(() => expect(button).toBeEnabled());
  await user.click(button);
  expect(await screen.findByText('No continuity issues were found.')).toBeVisible();
  expect(screen.getByText('Analysis complete.')).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Reload saved findings' }));
  expect(createAnalysis).toHaveBeenCalledTimes(1);
  expect(screen.queryByText('No continuity analysis has been run yet.')).not.toBeInTheDocument();
});

it('shows a durable failed analysis rather than a successful empty review', async () => {
  vi.mocked(listFindings).mockResolvedValue([]);
  vi.mocked(getAnalysis).mockResolvedValue({ ...run, status: 'FAILED', failureCode: 'PROVIDER_UNAVAILABLE', failureMessage: 'The provider was unavailable.' });
  open('?analysisId=run-1');
  expect(await screen.findByRole('alert')).toHaveTextContent('Analysis failed');
  expect(screen.getByRole('alert')).toHaveTextContent('The provider was unavailable.');
  expect(screen.queryByText('No continuity issues were found.')).not.toBeInTheDocument();
});

it('retries a failed findings read and reports a missing deep link', async () => {
  vi.mocked(listFindings).mockRejectedValueOnce(new ApiError(503));
  const user = userEvent.setup();
  open('?finding=missing');
  expect(await screen.findByRole('alert')).toHaveTextContent('Findings unavailable');
  await user.click(screen.getByRole('button', { name: 'Retry loading findings' }));
  expect(await screen.findByText('Finding not available')).toBeVisible();
  expect(await screen.findByRole('button', { name: /Color drift/ })).toBeVisible();
});

it('does not claim clipboard success until writeText resolves and supports keyboard activation', async () => {
  const user = userEvent.setup();
  let finish!: () => void;
  const copy = vi.spyOn(navigator.clipboard, 'writeText').mockImplementation(() => new Promise(resolve => { finish = resolve; }));
  open('?finding=finding-1');
  const button = await screen.findByRole('button', { name: 'Copy correction' });
  button.focus();
  await user.keyboard('{Enter}');
  expect(copy).toHaveBeenCalledWith(finding.suggestedCorrectionPrompt);
  expect(screen.queryByText('Correction copied.')).not.toBeInTheDocument();
  finish();
  expect(await screen.findByText('Correction copied.')).toBeVisible();
});

it('reports clipboard failure and allows a successful retry', async () => {
  const user = userEvent.setup();
  vi.spyOn(navigator.clipboard, 'writeText').mockRejectedValueOnce(new Error('denied')).mockResolvedValueOnce();
  open('?finding=finding-1');
  await user.click(await screen.findByRole('button', { name: 'Copy correction' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not copy.');
  expect(screen.queryByText('Correction copied.')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Copy correction' }));
  expect(await screen.findByText('Correction copied.')).toBeVisible();
});

it('maps only linked frames in shot/frame order and prefers different shots for the main pair', async () => {
  const multiple = { ...finding, affectedShotIds: ['shot-0', 'shot-1', 'shot-2'], relevantFrameIds: ['frame-2', 'frame-0b', 'frame-1', 'frame-0'] };
  const sequence = [{ ...shots[0]!, frames: [...shots[0]!.frames, { ...shots[0]!.frames[0]!, id: 'frame-0b', position: 1 }] }, shots[1]!, shots[2]!];
  expect(mapEvidence(multiple, [...sequence].reverse()).map(item => item.frame.id)).toEqual(['frame-0', 'frame-0b', 'frame-1', 'frame-2']);
  vi.mocked(listShots).mockResolvedValue(sequence);
  vi.mocked(listFindings).mockResolvedValue([multiple]);
  const user = userEvent.setup();
  open('?finding=finding-1');
  const viewer = await screen.findByRole('region', { name: 'Finding evidence' });
  expect(within(viewer).getByRole('img', { name: 'Evidence: shot-1.png, frame 1' })).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Shot 3 / frame 1' }));
  expect(within(viewer).getByRole('img', { name: 'Evidence: shot-2.png, frame 1' })).toBeVisible();
  expect(within(viewer).queryByRole('img', { name: 'Evidence: shot-1.png, frame 1' })).not.toBeInTheDocument();
});

it('handles one, missing and zero mapped evidence frames without substituting unrelated media', async () => {
  vi.mocked(listFindings).mockResolvedValue([{ ...finding, relevantFrameIds: ['frame-0', 'missing'] }]);
  const first = open('?finding=finding-1');
  expect(await screen.findByText(/Only one evidence frame/)).toBeVisible();
  expect(screen.getByRole('alert')).toHaveTextContent('1 evidence frame(s) unavailable');
  first.unmount();
  vi.mocked(listFindings).mockResolvedValue([{ ...finding, relevantFrameIds: ['frame-2'] }]);
  open('?finding=finding-1');
  expect(await screen.findByText('No evidence frames are available for this finding.')).toBeVisible();
  expect(screen.queryByRole('img', { name: /Evidence:/ })).not.toBeInTheDocument();
});

it('shows failed image loading with a real retry', async () => {
  const user = userEvent.setup();
  open('?finding=finding-1');
  const image = await screen.findByRole('img', { name: 'Evidence: shot-0.png, frame 1' });
  fireEvent.error(image);
  expect(screen.getByRole('alert')).toHaveTextContent('Evidence image unavailable');
  await user.click(screen.getByRole('button', { name: 'Retry image' }));
  expect(screen.getByRole('img', { name: 'Evidence: shot-0.png, frame 1' })).toHaveAttribute('src', '/frame-0.png');
});

it('paginates saved history and clears the old selection', async () => {
  vi.mocked(listFindings).mockResolvedValueOnce(Array.from({ length: 20 }, (_, index) => ({ ...finding, id: `finding-${index}` }))).mockResolvedValueOnce([]);
  const user = userEvent.setup();
  open('?finding=finding-1');
  await user.click(await screen.findByRole('button', { name: 'Next findings' }));
  expect(await screen.findByText('No saved findings here.')).toBeVisible();
  expect(screen.getByLabelText('Current URL')).toHaveTextContent('findingsPage=1');
  expect(screen.getByLabelText('Current URL')).not.toHaveTextContent('finding=');
  expect(listFindings).toHaveBeenLastCalledWith('project', undefined, 1, expect.any(AbortSignal));
});

it('announces running analysis and status lookup failures without retrying analysis', async () => {
  vi.mocked(getAnalysis).mockResolvedValueOnce({ ...run, status: 'RUNNING', completedAt: null }).mockRejectedValueOnce(new ApiError(404));
  vi.mocked(listFindings).mockResolvedValue([]);
  const user = userEvent.setup();
  open('?analysisId=run-1');
  expect(await screen.findByText(/Analysis is running/)).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Reload saved findings' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Analysis status unavailable');
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('reports a shots API failure without calling the evidence empty', async () => {
  vi.mocked(listShots).mockRejectedValue(new ApiError(503));
  open('?finding=finding-1');
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not load shots');
  expect(screen.queryByText('Your sequence belongs here.')).not.toBeInTheDocument();
  expect(screen.queryByText('No evidence frames are available for this finding.')).not.toBeInTheDocument();
});

it('compares two evidence frames within one shot and preserves video timestamps', async () => {
  const video: Shot = { ...shots[0]!, kind: 'VIDEO', durationMs: 3000, frames: [{ ...shots[0]!.frames[0]!, timestampMs: 250 }, { ...shots[0]!.frames[0]!, id: 'frame-video', position: 1, timestampMs: 2250, url: '/second.png' }] };
  vi.mocked(listShots).mockResolvedValue([video]);
  vi.mocked(listFindings).mockResolvedValue([{ ...finding, affectedShotIds: [video.id], relevantFrameIds: ['frame-video', 'frame-0'] }]);
  open('?finding=finding-1');
  const viewer = await screen.findByRole('region', { name: 'Finding evidence' });
  expect(within(viewer).getAllByRole('img')).toHaveLength(2);
  expect(within(viewer).getByText('Frame 1 · 0.250 s')).toBeVisible();
  expect(within(viewer).getByText('Frame 2 · 2.250 s')).toBeVisible();
});

it('resets clipboard feedback when selecting another finding and preserves analysis-scoped reload links', async () => {
  vi.mocked(listFindings).mockResolvedValue([finding, { ...finding, id: 'finding-2', title: 'Second issue', suggestedCorrectionPrompt: 'Second correction.' }]);
  const user = userEvent.setup();
  vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue();
  open('?finding=finding-1');
  await user.click(await screen.findByRole('button', { name: 'Copy correction' }));
  expect(await screen.findByText('Correction copied.')).toBeVisible();
  await user.click(screen.getByRole('button', { name: /Second issue/ }));
  expect(await screen.findByText('Second correction.')).toBeVisible();
  expect(screen.queryByText('Correction copied.')).not.toBeInTheDocument();
  expect(screen.getByLabelText('Current URL')).toHaveTextContent('analysisId=run-1');
  expect(screen.getByLabelText('Current URL')).toHaveTextContent('finding=finding-2');
  await user.click(screen.getByRole('button', { name: 'All saved findings' }));
  expect(screen.queryByRole('heading', { name: 'Finding evidence' })).not.toBeInTheDocument();
});
