import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import { archiveReference, createAnalysis, listFindingReferences, listReferences, updateProjectRules, updateReference, uploadReference, type Finding, type Project, type Reference } from '@sceneproof/api-client';
import { Providers } from '../providers';
import { ReferenceBible } from './ReferenceBible';
import { FindingReferences } from './FindingReferences';

vi.mock('@sceneproof/api-client', async importOriginal => ({ ...await importOriginal<typeof import('@sceneproof/api-client')>(), archiveReference: vi.fn(), createAnalysis: vi.fn(), listFindingReferences: vi.fn(), listReferences: vi.fn(), updateProjectRules: vi.fn(), updateReference: vi.fn(), uploadReference: vi.fn() }));
const project: Project = { id: 'project', name: 'Film', description: '', rules: 'Keep red.', createdAt: '2026-09-13T12:00:00Z', updatedAt: '2026-09-13T12:00:00Z' };
const reference: Reference = { id: 'reference', projectId: 'project', title: 'Red prop', guidance: 'Keep this red shape.', width: 128, height: 128, sha256: 'a'.repeat(64), createdAt: project.createdAt, archivedAt: null, url: '/api/v1/projects/project/references/reference/content' };
const finding: Finding = { id: 'finding', analysisRunId: 'run', category: 'PROP', severity: 'HIGH', confidence: 0.9, title: 'Color drift', summary: 'Drift', expectedState: 'Red', observedState: 'Blue', explanation: 'Declared red', affectedShotIds: ['shot'], relevantFrameIds: ['frame'], relevantReferenceIds: [reference.id], suggestedCorrectionPrompt: 'Keep red.', status: 'OPEN' };
function open() { return render(<Providers><ReferenceBible project={project} /></Providers>); }
function evidence(ids = finding.relevantReferenceIds) { return render(<Providers><FindingReferences projectId={project.id} finding={{ ...finding, relevantReferenceIds: ids }} /></Providers>); }
beforeEach(() => {
  vi.mocked(listReferences).mockResolvedValue([]);
  vi.mocked(uploadReference).mockResolvedValue(reference);
  vi.mocked(updateReference).mockResolvedValue(reference);
  vi.mocked(archiveReference).mockResolvedValue({ ...reference, archivedAt: project.createdAt });
  vi.mocked(listFindingReferences).mockResolvedValue([reference]);
  vi.mocked(updateProjectRules).mockResolvedValue({ ...project, rules: 'New rules' });
});

it('saves edited rules with real pending and saved states and no analysis', async () => {
  let resolve!: (saved: Project) => void;
  vi.mocked(updateProjectRules).mockReturnValue(new Promise(done => { resolve = done; }));
  const user = userEvent.setup(); open();
  await user.clear(screen.getByLabelText('Continuity rules'));
  await user.type(screen.getByLabelText('Continuity rules'), 'New rules');
  await user.click(screen.getByRole('button', { name: 'Save rules' }));
  expect(await screen.findByText('Saving rules…')).toBeVisible();
  expect(screen.queryByText('Rules saved.')).not.toBeInTheDocument();
  await act(async () => resolve({ ...project, rules: 'New rules' }));
  expect(await screen.findByText('Rules saved.')).toBeVisible();
  expect(updateProjectRules).toHaveBeenCalledWith(project.id, { rules: 'New rules' });
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('retains rules after save failure and allows retry', async () => {
  vi.mocked(updateProjectRules).mockRejectedValueOnce(new Error('Rules storage unavailable.'));
  const user = userEvent.setup(); open();
  await user.clear(screen.getByLabelText('Continuity rules'));
  await user.type(screen.getByLabelText('Continuity rules'), 'New rules');
  await user.click(screen.getByRole('button', { name: 'Save rules' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Rules storage unavailable.');
  expect(screen.getByLabelText('Continuity rules')).toHaveValue('New rules');
  expect(screen.queryByText('Rules saved.')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Save rules' }));
  expect(await screen.findByText('Rules saved.')).toBeVisible();
});

it('reference list failure is isolated and can be retried', async () => {
  vi.mocked(listReferences).mockRejectedValueOnce(new Error('Reference list offline.'));
  const user = userEvent.setup(); open();
  expect(await screen.findByRole('alert')).toHaveTextContent('Reference list offline.');
  expect(screen.getByLabelText('Continuity rules')).toBeEnabled();
  expect(screen.getByRole('button', { name: '+ Add visual reference' })).toBeDisabled();
  await user.click(screen.getByRole('button', { name: 'Retry references' }));
  expect(await screen.findByText('No visual references yet.')).toBeVisible();
});

async function add() {
  const user = userEvent.setup({ applyAccept: false });
  await waitFor(() => expect(screen.getByRole('button', { name: '+ Add visual reference' })).toBeEnabled());
  await user.click(screen.getByRole('button', { name: '+ Add visual reference' }));
  return user;
}

it('validates title file type and file size before upload', async () => {
  open(); const user = await add();
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByText('Give this reference a title.')).toBeVisible();
  await user.type(screen.getByLabelText('Reference title'), reference.title);
  for (const file of [new File(['x'], 'video.mp4', { type: 'video/mp4' }), new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'big.png', { type: 'image/png' })]) {
    await user.upload(document.querySelector('input[type="file"]') as HTMLInputElement, file);
    await user.click(screen.getByRole('button', { name: 'Save reference' }));
    expect(await screen.findByText('Choose a JPEG or PNG image up to 10 MiB.')).toBeVisible();
  }
  expect(uploadReference).not.toHaveBeenCalled();
});

it('uploads with pending state then shows the normalized backend thumbnail', async () => {
  let resolve!: (saved: Reference) => void;
  vi.mocked(uploadReference).mockReturnValue(new Promise(done => { resolve = done; }));
  open(); const user = await add();
  const file = new File(['fixture'], 'reference.png', { type: 'image/png' });
  await user.upload(document.querySelector('input[type="file"]') as HTMLInputElement, file);
  await user.type(screen.getByLabelText('Reference title'), reference.title);
  await user.type(screen.getByLabelText('Creator guidance'), reference.guidance);
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByText('Uploading and validating reference…')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled();
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
  await act(async () => resolve(reference));
  expect(await screen.findByRole('img', { name: 'Reference: Red prop' })).toHaveAttribute('src', reference.url);
  expect(uploadReference).toHaveBeenCalledWith(project.id, file, { title: reference.title, guidance: reference.guidance });
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('upload failure retains input and never claims successful upload', async () => {
  vi.mocked(uploadReference).mockRejectedValueOnce(new Error('Corrupt image.'));
  open(); const user = await add();
  await user.upload(document.querySelector('input[type="file"]') as HTMLInputElement, new File(['bad'], 'bad.png', { type: 'image/png' }));
  await user.type(screen.getByLabelText('Reference title'), reference.title);
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Corrupt image.');
  expect(screen.getByLabelText('Reference title')).toHaveValue(reference.title);
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByRole('img')).toHaveAttribute('src', reference.url);
});

it('inspection supports keyboard focus and metadata save errors', async () => {
  vi.mocked(listReferences).mockResolvedValue([reference]);
  vi.mocked(updateReference).mockRejectedValueOnce(new Error('Update unavailable.')).mockResolvedValueOnce({ ...reference, title: 'Edited prop' });
  open(); const user = userEvent.setup();
  const inspect = await screen.findByRole('button', { name: 'Inspect reference: Red prop' });
  inspect.focus(); await user.keyboard('{Enter}');
  expect(await screen.findByRole('dialog')).toBeVisible();
  await user.clear(screen.getByLabelText('Reference title'));
  await user.type(screen.getByLabelText('Reference title'), 'Edited prop');
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Update unavailable.');
  await user.click(screen.getByRole('button', { name: 'Save reference' }));
  expect(await screen.findByRole('button', { name: 'Inspect reference: Edited prop' })).toBeVisible();
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('archive failure preserves the active Bible then successful archive removes it', async () => {
  vi.mocked(listReferences).mockResolvedValue([reference]);
  vi.mocked(archiveReference).mockRejectedValueOnce(new Error('Archive unavailable.')).mockResolvedValueOnce({ ...reference, archivedAt: project.createdAt });
  open(); const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: 'Inspect reference: Red prop' }));
  await user.click(screen.getByRole('button', { name: 'Archive reference' }));
  await user.click(screen.getByRole('button', { name: 'Confirm archive' }));
  expect(await screen.findByText('Archive unavailable.')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Inspect reference: Red prop', hidden: true })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Confirm archive' }));
  expect(await screen.findByText('No visual references yet.')).toBeVisible();
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('cited archived reference shows historical guidance independently of shot evidence', async () => {
  vi.mocked(listFindingReferences).mockResolvedValue([{ ...reference, archivedAt: project.createdAt }]);
  evidence();
  expect(await screen.findByRole('img', { name: 'Reference: Red prop' })).toHaveAttribute('src', reference.url);
  expect(screen.getByText(reference.guidance)).toBeVisible();
  expect(screen.getByText(/Archived from the current Bible/)).toBeVisible();
  expect(createAnalysis).not.toHaveBeenCalled();
});

it('a finding without citations neither loads nor fabricates reference evidence', () => {
  evidence([]);
  expect(screen.queryByRole('heading', { name: 'Reference evidence' })).not.toBeInTheDocument();
  expect(listFindingReferences).not.toHaveBeenCalled();
});

it('historical list failure offers retry and incomplete evidence remains honest', async () => {
  vi.mocked(listFindingReferences).mockRejectedValueOnce(new Error('Offline')).mockResolvedValueOnce([]);
  evidence(); const user = userEvent.setup();
  expect(await screen.findByRole('alert')).toHaveTextContent('Historical references unavailable');
  await user.click(screen.getByRole('button', { name: 'Retry reference evidence' }));
  await waitFor(() => expect(listFindingReferences).toHaveBeenCalledTimes(2));
  expect(screen.getByRole('alert')).toHaveTextContent('could not be fully loaded');
  expect(screen.queryByRole('img')).not.toBeInTheDocument();
});

it('image failure does not substitute current evidence', async () => {
  evidence();
  fireEvent.error(await screen.findByRole('img'));
  expect(await screen.findByRole('alert')).toHaveTextContent('Historical evidence has not been replaced.');
});

it('eight active references disable addition without hiding existing images', async () => {
  vi.mocked(listReferences).mockResolvedValue(Array.from({ length: 8 }, (_, index) => ({ ...reference, id: `ref-${index}`, title: `Prop ${index}` })));
  open();
  await screen.findByRole('button', { name: 'Inspect reference: Prop 7' });
  expect(screen.getByRole('button', { name: '+ Add visual reference' })).toBeDisabled();
  expect(within(screen.getByRole('list')).getAllByRole('img')).toHaveLength(8);
});
