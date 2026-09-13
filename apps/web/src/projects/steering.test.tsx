import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import { createFindingAction, listFindingActions, type Finding, type FindingAction } from '@sceneproof/api-client';
import { Providers } from '../providers';
import { FindingActions } from './FindingActions';

vi.mock('@sceneproof/api-client', async importOriginal => ({ ...await importOriginal<typeof import('@sceneproof/api-client')>(), createFindingAction: vi.fn(), listFindingActions: vi.fn() }));
const finding: Finding = { id: 'finding', analysisRunId: 'original-run', category: 'PROP', severity: 'HIGH', confidence: 0.9, title: 'Color drift', summary: 'Red to blue', expectedState: 'Red', observedState: 'Blue', explanation: 'The rule requires red', suggestedCorrectionPrompt: 'Keep red', affectedShotIds: ['shot-1', 'shot-2'], relevantFrameIds: ['frame-1', 'frame-2'], relevantReferenceIds: [], status: 'OPEN' };
const action: FindingAction = { id: 'action', projectId: 'project', findingId: finding.id, originalAnalysisRunId: finding.analysisRunId, requestId: 'request', type: 'INTENTIONAL_CHANGE', explanation: 'The prop is repainted at home.', scope: 'Between shots 1 and 2', affectedShotIds: finding.affectedShotIds, createdAt: '2026-09-12T22:00:00Z', supersedesActionId: null,
  reanalysis: { id: 'targeted-run', kind: 'TARGETED', projectId: 'project', requestId: 'request', status: 'SUCCEEDED', provider: 'OPENAI', model: 'gpt-6-astra', startedAt: '2026-09-12T22:00:00Z', completedAt: '2026-09-12T22:01:00Z', failureCode: null, failureMessage: null, shotCount: 2, frameCount: 2, projectSummary: 'Narrative transition', warnings: [], usage: null, providerRequestId: null, providerResponseId: null },
  result: { projectId: 'project', originalFindingId: finding.id, schemaVersion: '1', outcome: 'INTENT_ACCEPTED', summary: 'Narrative transition', explanation: 'The transition explains the visual difference.', evaluatedScope: 'After arriving home', affectedShotIds: finding.affectedShotIds, evidenceFrameIds: finding.relevantFrameIds, inspectedShots: [{ shotId: 'shot-1', frameIds: ['frame-1'] }, { shotId: 'shot-2', frameIds: ['frame-2'] }], remainingIssue: '', suggestedCorrection: '' },
};
function open(status: Finding['status'] = 'OPEN') { return render(<Providers><FindingActions projectId="project" finding={{ ...finding, status }} /></Providers>); }
beforeEach(() => { sessionStorage.clear(); vi.mocked(listFindingActions).mockResolvedValue([]); vi.mocked(createFindingAction).mockResolvedValue(action); });

async function fillIntent() {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'This change is intentional' }));
  await user.type(screen.getByLabelText(/Why is this change intentional/), action.explanation);
  await user.type(screen.getByLabelText(/Narrative scope/), action.scope);
  return user;
}

it('requires narrative explanation and scope before an explicit provider confirmation', async () => {
  open();
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'This change is intentional' }));
  fireEvent.submit(screen.getByRole('button', { name: 'Confirm intent & re-evaluate' }).closest('form')!);
  expect(await screen.findByText(/Provide an explanation and the narrative scope/)).toBeVisible();
  expect(createFindingAction).not.toHaveBeenCalled();
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
});

it('announces real pending state and persists UUID before one explicit call', async () => {
  let finish!: (value: FindingAction) => void;
  vi.mocked(createFindingAction).mockReturnValue(new Promise(resolve => { finish = resolve; }));
  open(); const user = await fillIntent();
  await user.click(screen.getByRole('button', { name: 'Confirm intent & re-evaluate' }));
  expect(screen.getByRole('status')).toHaveTextContent('Re-evaluating the original evidence');
  expect(screen.queryByRole('button', { name: 'Confirm intent & re-evaluate' })).not.toBeInTheDocument();
  const body = vi.mocked(createFindingAction).mock.calls[0]![2];
  expect(body).toMatchObject({ type: 'INTENTIONAL_CHANGE', explanation: action.explanation, scope: action.scope, affectedShotIds: finding.affectedShotIds });
  expect(JSON.parse(sessionStorage.getItem('sceneproof-action:project:finding')!)).toEqual(body);
  vi.mocked(listFindingActions).mockResolvedValue([action]); finish(action);
  expect(await screen.findByText('Creator action saved.')).toBeVisible();
  expect(createFindingAction).toHaveBeenCalledTimes(1);
});

it.each([
  ['INTENT_ACCEPTED', 'INTENTIONAL', 'Intent explains the difference'],
  ['ISSUE_REMAINS', 'OPEN', 'The continuity issue remains'],
  ['INSUFFICIENT_EVIDENCE', 'OPEN', 'Insufficient evidence to decide'],
] as const)('renders %s honestly with preserved original history on reload', async (outcome, status, title) => {
  vi.mocked(listFindingActions).mockResolvedValue([{ ...action, result: { ...action.result!, outcome, remainingIssue: outcome === 'ISSUE_REMAINS' ? 'Rule conflict persists' : '', suggestedCorrection: outcome === 'ISSUE_REMAINS' ? 'Keep it red' : '' } }]);
  open(status);
  expect(await screen.findByRole('alert', { name: title })).toBeVisible();
  await userEvent.click(screen.getByText(/View immutable history/));
  expect(screen.getByText(/Original analysis: original-run/)).toBeVisible();
  expect(screen.getByText(`Creator: ${action.explanation}`)).toBeVisible();
  expect(createFindingAction).not.toHaveBeenCalled();
});

it('recovers network failure after reload with exactly the original request UUID and payload', async () => {
  vi.mocked(createFindingAction).mockRejectedValueOnce(new Error('Connection lost'));
  const mounted = open(); const user = await fillIntent();
  await user.click(screen.getByRole('button', { name: 'Confirm intent & re-evaluate' }));
  expect(await screen.findByText(/Connection lost/)).toBeVisible();
  const original = vi.mocked(createFindingAction).mock.calls[0]![2];
  mounted.unmount(); open();
  await user.click(screen.getByRole('button', { name: 'Recover same request · replay-safe' }));
  await waitFor(() => expect(createFindingAction).toHaveBeenCalledTimes(2));
  expect(vi.mocked(createFindingAction).mock.calls[1]![2]).toEqual(original);
  expect(await screen.findByText('Creator action saved.')).toBeVisible();
});

it('durable failure remains visible and refresh only reads history', async () => {
  vi.mocked(listFindingActions).mockResolvedValue([{ ...action, result: null, reanalysis: { ...action.reanalysis!, status: 'FAILED', failureMessage: 'Provider timed out.' } }]);
  open(); await userEvent.click(screen.getByText(/View immutable history/));
  expect(await screen.findByText(/Provider timed out/)).toBeVisible();
  await userEvent.click(screen.getByRole('button', { name: 'Refresh history' }));
  expect(createFindingAction).not.toHaveBeenCalled();
  expect(await screen.findByRole('button', { name: 'This change is intentional' })).toBeEnabled();
});

it.each([['RESOLVE', 'Resolve', 'Confirm resolve', 'RESOLVED'], ['DISMISS', 'Dismiss', 'Confirm dismiss', 'DISMISSED']] as const)('saves %s with a creator note and distinct durable status', async (type, label, confirm, status) => {
  vi.mocked(createFindingAction).mockResolvedValue({ ...action, type, reanalysis: null, result: null });
  const mounted = open(); const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: label }));
  await user.type(screen.getByLabelText(/Creator note/), 'Creator decision with audit note.');
  await user.click(screen.getByRole('button', { name: confirm }));
  await waitFor(() => expect(createFindingAction).toHaveBeenCalledWith('project', 'finding', expect.objectContaining({ type, explanation: 'Creator decision with audit note.' })));
  expect(await screen.findByText('Creator action saved.')).toBeVisible();
  mounted.unmount(); open(status);
  expect(screen.getByText(status)).toBeVisible();
  expect(screen.queryByRole('button', { name: 'This change is intentional' })).not.toBeInTheDocument();
});

it('restores running work through GET and blocks another creator action', async () => {
  vi.mocked(listFindingActions).mockResolvedValue([{ ...action, result: null, reanalysis: { ...action.reanalysis!, status: 'RUNNING', completedAt: null } }]);
  open();
  expect(await screen.findByText(/Re-evaluating the original evidence/)).toBeVisible();
  expect(screen.getByRole('button', { name: 'This change is intentional' })).toBeDisabled();
  expect(createFindingAction).not.toHaveBeenCalled();
});
