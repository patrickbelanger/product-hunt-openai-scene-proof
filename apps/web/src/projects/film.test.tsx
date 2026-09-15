import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import * as api from '@sceneproof/api-client';
import { Providers } from '../providers';
import { FilmIntelligence } from './FilmIntelligence';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  getFilmIntelligence: vi.fn(), uploadSourceFilm: vi.fn(), understandFilm: vi.fn(), getFilmCandidates: vi.fn(), getFilmTranscript: vi.fn(), decideFilmCandidate: vi.fn(), promoteFilmCandidate: vi.fn(),
}));
const projectId = 'c2bca20c-5964-4a4f-a4f5-c11659823f08';
const source = { id: 'd2bca20c-5964-4a4f-a4f5-c11659823f08', projectId, name: 'Original source', sha256: 'a'.repeat(64), byteSize: 1000, durationMs: 3000, createdAt: '2026-09-14T01:00:00Z' };
const run: api.FilmRun = { id: 'e2bca20c-5964-4a4f-a4f5-c11659823f08', projectId, sourceFilmId: source.id, requestId: 'f2bca20c-5964-4a4f-a4f5-c11659823f08', stage: 'UNDERSTANDING_FILM', startedAt: source.createdAt, completedAt: null, failureCode: null, failureMessage: null, stages: [{ stage: 'UNDERSTANDING_FILM', startedAt: source.createdAt, completedAt: null }], model: 'gpt-6-astra', reasoning: 'medium', transcriptionModel: 'whisper-1', audioStatus: 'TRANSCRIBED', audioDurationMs: 3000, transcriptionRequestId: null, providerResponseId: null, providerRequestId: null, usage: null, result: null };
const candidate: api.FilmCandidate = { id: 'candidate', runId: run.id, proposal: { title: 'Red shape', rule: 'Keep the shape red', scope: 'Same room', entityIds: [], evidence: { frameIds: ['frame'], transcriptSegmentIds: ['transcript'] }, uncertainty: 'Lighting may vary' }, status: 'PENDING', confirmedTitle: null, confirmedRule: null, confirmedScope: null, decidedAt: null, referenceId: null };
const complete: api.FilmRun = { ...run, stage: 'SUCCEEDED', completedAt: source.createdAt, stages: [], result: { projectId, sourceFilmId: source.id, schemaVersion: '1', summary: 'A real response summary supplied through the API', inspectedSegmentIds: ['segment'], inspectedFrameIds: ['frame'], entities: [], candidates: [candidate.proposal], narrativeCues: [], potentialConcerns: [], warnings: ['Sampled coverage'] } };
let state: api.FilmIntelligence;
let savedCandidates: api.FilmCandidate[];
function open() { return render(<Providers><MemoryRouter><FilmIntelligence projectId={projectId} /></MemoryRouter></Providers>); }
beforeEach(() => {
  sessionStorage.clear();
  state = { source, runs: [], segments: [{ id: 'segment', position: 0, startMs: 0, endMs: 3000, shot: { id: 'segment', position: 0, name: 'Film segment', kind: 'VIDEO', status: 'READY', failureCode: null, durationMs: 3000, frames: [{ id: 'frame', position: 0, timestampMs: 500, width: 32, height: 32, url: '/frame.png' }] } }], confirmedAnchors: [] };
  savedCandidates = [candidate];
  vi.mocked(api.getFilmIntelligence).mockImplementation(async () => state);
  vi.mocked(api.getFilmCandidates).mockImplementation(async () => savedCandidates);
  vi.mocked(api.getFilmTranscript).mockResolvedValue([{ id: 'transcript', startMs: 100, endMs: 900, text: 'Original spoken context', timestampOrigin: 'WHISPER_SEGMENT_ESTIMATE_SOURCE_START' }]);
  vi.mocked(api.understandFilm).mockImplementation(async (_project, request) => { const started = { ...run, requestId: request.requestId }; state = { ...state, runs: [started] }; return started; });
  vi.mocked(api.uploadSourceFilm).mockImplementation(async () => { state = { ...state, source }; return source; });
  vi.mocked(api.decideFilmCandidate).mockImplementation(async (_project, _candidate, decision) => {
    const saved = { ...candidate, status: decision.status, confirmedTitle: decision.title || candidate.proposal.title, confirmedRule: decision.rule || candidate.proposal.rule, confirmedScope: decision.scope || candidate.proposal.scope };
    savedCandidates = [saved]; return saved;
  });
  vi.mocked(api.promoteFilmCandidate).mockImplementation(async () => { const saved = { ...savedCandidates[0]!, referenceId: 'reference' }; savedCandidates = [saved]; return saved; });
});
afterEach(() => vi.restoreAllMocks());

it('uploading a bounded primary film makes no paid request', async () => {
  state = { ...state, source: null };
  const user = userEvent.setup(); const view = open();
  await screen.findByText('Primary source: MP4/H.264, up to 120 seconds and 100 MiB. Uploading makes no AI call.');
  const file = new File(['original'], 'original.mp4', { type: 'video/mp4' });
  await user.upload(view.container.querySelector('input[type=file]')!, file);
  await user.click(screen.getByRole('button', { name: 'Upload source film' }));
  await screen.findByRole('button', { name: 'Understand film' });
  expect(api.uploadSourceFilm).toHaveBeenCalledWith(projectId, file);
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('requires explicit consent and records request identity before the single submission', async () => {
  const user = userEvent.setup(); open();
  await user.click(await screen.findByRole('button', { name: 'Understand film' }));
  expect(api.understandFilm).not.toHaveBeenCalled();
  await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Cancel' }));
  expect(api.understandFilm).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: 'Understand film' }));
  await user.dblClick(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Confirm & understand' }));
  await screen.findByText('Understanding visual and narrative context', { selector: '[role=status]' });
  expect(api.understandFilm).toHaveBeenCalledTimes(1);
  expect(api.understandFilm).toHaveBeenCalledWith(projectId, expect.objectContaining({ sourceFilmId: source.id, paidConsent: true, requestId: expect.any(String) }));
  expect(screen.getByRole('button', { name: 'New understanding attempt' })).toBeDisabled();
});

it('reload recovers actual stages with no new paid work or invented percentage', async () => {
  state = { ...state, runs: [run] }; const view = open();
  await screen.findByText('Understanding visual and narrative context', { selector: '[role=status]' });
  expect(screen.queryByText(/\d+%|AI is thinking/i)).not.toBeInTheDocument();
  view.unmount(); open();
  await screen.findByText('Understanding visual and narrative context', { selector: '[role=status]' });
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('connection loss reuses the saved UUID after remount and prevents a fresh attempt', async () => {
  vi.mocked(api.understandFilm).mockRejectedValueOnce(new Error('Connection lost'));
  const user = userEvent.setup(); const view = open();
  await user.click(await screen.findByRole('button', { name: 'Understand film' }));
  await user.click(await screen.findByRole('button', { name: 'Confirm & understand' }));
  await screen.findByText('Connection lost');
  const first = vi.mocked(api.understandFilm).mock.calls[0]![1];
  view.unmount(); open();
  await screen.findByRole('button', { name: 'Recover same film request' });
  expect(screen.getByRole('button', { name: 'Understand film' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: 'Recover same film request' }));
  await waitFor(() => expect(api.understandFilm).toHaveBeenCalledTimes(2));
  expect(vi.mocked(api.understandFilm).mock.calls[1]![1]).toEqual(first);
});

it('candidate acceptance and reference promotion are explicit non-paid actions with inspectable evidence', async () => {
  state = { ...state, runs: [complete] }; const user = userEvent.setup(); open();
  await user.click(await screen.findByRole('button', { name: 'Accept anchor' }));
  await screen.findByText('Creator-confirmed memory for later continuity review.');
  await user.click(screen.getByText(/Inspect supporting evidence/));
  expect(screen.getByRole('img', { name: 'Source film at 0.50s' })).toHaveAttribute('src', '/frame.png');
  expect((await screen.findAllByText('Original spoken context'))[0]).toBeVisible();
  await user.click(screen.getByRole('button', { name: 'Add visual reference to Bible' }));
  await screen.findByRole('button', { name: 'Added to Reference Bible' });
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('edited creator wording is submitted as a confirmed scoped anchor', async () => {
  state = { ...state, runs: [complete] }; const user = userEvent.setup(); open();
  await user.click(await screen.findByRole('button', { name: 'Edit anchor' }));
  fireEvent.change(await screen.findByLabelText('Continuity expectation'), { target: { value: 'Creator expectation' } });
  await user.click(await screen.findByRole('button', { name: 'Confirm edited anchor' }));
  await waitFor(() => expect(api.decideFilmCandidate).toHaveBeenCalledWith(projectId, candidate.id, { status: 'EDITED', title: 'Red shape', rule: 'Creator expectation', scope: 'Same room' }));
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('rejected candidates never offer visual promotion or claim confirmed authority', async () => {
  state = { ...state, runs: [complete] }; const user = userEvent.setup(); open();
  await user.click(await screen.findByRole('button', { name: 'Reject anchor' }));
  await screen.findByText('REJECTED');
  expect(screen.queryByRole('button', { name: 'Add visual reference to Bible' })).not.toBeInTheDocument();
  expect(screen.queryByText('Creator-confirmed memory for later continuity review.')).not.toBeInTheDocument();
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('failed history remains visible and a new attempt requires a new consent dialog', async () => {
  state = { ...state, runs: [{ ...run, stage: 'FAILED', completedAt: source.createdAt, failureCode: 'TRANSCRIPTION_UNAVAILABLE', failureMessage: 'Recorded provider failure' }] };
  const user = userEvent.setup(); open();
  await screen.findByText(/Recorded provider failure/);
  await user.click(screen.getByRole('button', { name: 'New understanding attempt' }));
  expect(await screen.findByRole('dialog')).toHaveTextContent('This paid action');
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('storage failure sends no untracked paid request', async () => {
  const user = userEvent.setup(); open();
  await user.click(await screen.findByRole('button', { name: 'Understand film' }));
  vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Unavailable'); });
  await user.click(await screen.findByRole('button', { name: 'Confirm & understand' }));
  expect(within(screen.getByRole('dialog')).getByRole('alert')).toHaveTextContent('No paid request was sent');
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('read failures remain visible and refresh is read-only', async () => {
  vi.mocked(api.getFilmIntelligence).mockRejectedValueOnce(new Error('Unavailable'));
  const user = userEvent.setup(); open();
  await screen.findByText(/last known state may be outdated/);
  await user.click(screen.getByRole('button', { name: 'Refresh now' }));
  await screen.findByRole('button', { name: 'Understand film' });
  expect(api.understandFilm).not.toHaveBeenCalled();
});

it('polls quietly, keeps manual refresh idle, then stops on persisted success', async () => {
  state = { ...state, runs: [run] };
  let resolvePoll!: (value: api.FilmIntelligence) => void;
  vi.mocked(api.getFilmIntelligence).mockResolvedValueOnce(state).mockImplementationOnce(() => new Promise(resolve => { resolvePoll = resolve; }));
  open(); await screen.findByText('Step 4 of 5');
  await waitFor(() => expect(resolvePoll).toBeDefined(), { timeout: 3000 });
  expect(screen.getByText('Syncing…')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Refresh now' })).not.toHaveAttribute('data-loading');
  await act(async () => resolvePoll({ ...state, runs: [complete] }));
  await screen.findByText('Saved · live updates stopped');
  const calls = vi.mocked(api.getFilmIntelligence).mock.calls.length;
  await act(async () => { await new Promise(resolve => setTimeout(resolve, 1700)); });
  expect(api.getFilmIntelligence).toHaveBeenCalledTimes(calls);
});

it('animates refresh only on an explicit click and leaves failed runs terminal', async () => {
  state = { ...state, runs: [{ ...run, stage: 'FAILED', completedAt: source.createdAt, failureMessage: 'Persistent failure' }] };
  let resolveRefresh!: (value: api.FilmIntelligence) => void;
  vi.mocked(api.getFilmIntelligence).mockResolvedValueOnce(state).mockImplementationOnce(() => new Promise(resolve => { resolveRefresh = resolve; }));
  open(); await screen.findByText(/Persistent failure/);
  const button = screen.getByRole('button', { name: 'Refresh now' }); fireEvent.click(button);
  await waitFor(() => expect(button).toHaveAttribute('data-loading'));
  await act(async () => resolveRefresh(state));
  await waitFor(() => expect(button).not.toHaveAttribute('data-loading'));
  await act(async () => { await new Promise(resolve => setTimeout(resolve, 1700)); });
  expect(api.getFilmIntelligence).toHaveBeenCalledTimes(2);
});

it('prioritizes exact summary and concerns while retaining expandable uncertainty, limitations and full evidence', async () => {
  state = { ...state, runs: [{ ...complete, result: { ...complete.result!, potentialConcerns: [{ title: 'Possible prop change', explanation: 'Could this be a different object?', uncertainty: 'Identity is uncertain', evidence: candidate.proposal.evidence }] } }] };
  const user = userEvent.setup(); open();
  expect(await screen.findByRole('heading', { name: 'Understanding summary' })).toBeVisible();
  expect(screen.getByText(complete.result!.summary)).toBeVisible();
  expect(screen.getByRole('heading', { name: 'Possible prop change' })).toBeVisible();
  expect(screen.getByText('Identity is uncertain')).not.toBeVisible();
  await user.click(screen.getByText('Concern rationale & uncertainty'));
  expect(screen.getByText('Identity is uncertain')).toBeVisible();
  await user.click(screen.getByText('Methodology & limitations'));
  expect(screen.getByText(/Only sampled frames were inspected/)).toBeVisible();
  expect(screen.getByText(/Transcript timestamps are approximate/)).toBeVisible();
  expect(screen.getByText(/Proposals require creator confirmation/)).toBeVisible();
  const concerns = screen.getByRole('region', { name: 'Potential continuity concerns' });
  await user.click(within(concerns).getByText(/Inspect supporting evidence/));
  await user.click(within(concerns).getByRole('button', { name: 'Enlarge source frame at 0.50s' }));
  expect(within(await screen.findByRole('dialog')).getByRole('img')).toHaveAttribute('src', '/frame.png');
  expect(api.understandFilm).not.toHaveBeenCalled();
});
