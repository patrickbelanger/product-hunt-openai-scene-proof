import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, expect, it, vi } from 'vitest';
import { ApiError, getProject, listFindings, listProjects, listReferences, listShots, openDemo, resetDemo, type Project } from '@sceneproof/api-client';
import { App } from '../App';
import { Providers } from '../providers';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  getFilmIntelligence: vi.fn().mockResolvedValue({ source: null, runs: [], segments: [], confirmedAnchors: [] }),
  openDemo: vi.fn(), resetDemo: vi.fn(), getProject: vi.fn(), listProjects: vi.fn(), listReferences: vi.fn(), listShots: vi.fn(), listFindings: vi.fn(),
}));
const project: Project = {
  id: 'b79b9b84-289e-480b-a266-3b9ff8aa4e38', name: 'An API-supplied demo title', description: 'Persisted description', rules: 'Persisted rules',
  createdAt: '2026-09-13T10:00:00Z', updatedAt: '2026-09-13T10:00:00Z',
  demo: { instanceId: 'ad44be41-2d70-463a-b045-f919e1f5de0a', templateVersion: 'test-only-v1', retired: false },
};
const fresh = { ...project, id: '7df3b38b-8514-48f4-965c-0c8c08535a20', name: 'Fresh API copy' };
function open(path = '/') { return render(<Providers><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></Providers>); }
beforeEach(() => {
  sessionStorage.clear(); localStorage.clear();
  vi.mocked(listProjects).mockResolvedValue({ items: [], page: 0, hasNext: false });
  vi.mocked(listReferences).mockResolvedValue([]); vi.mocked(listShots).mockResolvedValue([]); vi.mocked(listFindings).mockResolvedValue([]);
  vi.mocked(getProject).mockResolvedValue(project); vi.mocked(openDemo).mockResolvedValue(project); vi.mocked(resetDemo).mockResolvedValue(fresh);
});

it('landing exposes both paths and launch consumes the real project response without uploads or tour dependency', async () => {
  const user = userEvent.setup(); open();
  expect(screen.getByRole('link', { name: /create a project/i })).toHaveAttribute('href', '/projects/new');
  await user.click(screen.getByRole('button', { name: 'Try the demo film' }));
  expect(await screen.findByRole('heading', { name: project.name })).toBeInTheDocument();
  expect(screen.getByLabelText('Continuity rules', { exact: true })).toHaveValue(project.rules);
  expect(screen.getByText('Demo project')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Start tour' })).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  expect(screen.getByRole('heading', { name: project.name })).toHaveFocus();
  expect(screen.getByText('Import a shot').closest('details')).not.toHaveAttribute('open');
  expect(openDemo).toHaveBeenCalledTimes(1);
});

it('launch announces pending work and protects against duplicate clicks', async () => {
  let resolve!: (result: Project) => void;
  vi.mocked(openDemo).mockReturnValue(new Promise(done => { resolve = done; }));
  const user = userEvent.setup(); open();
  await user.dblClick(screen.getByRole('button', { name: 'Try the demo film' }));
  expect(screen.getByText('Preparing your demo copy…')).toHaveAttribute('role', 'status');
  expect(openDemo).toHaveBeenCalledTimes(1);
  expect(screen.queryByRole('heading', { name: project.name })).not.toBeInTheDocument();
  await act(async () => resolve(project));
  expect(await screen.findByRole('heading', { name: project.name })).toBeInTheDocument();
});

it('failure retry and remount reuse the durable request identity', async () => {
  vi.mocked(openDemo).mockRejectedValueOnce(new ApiError(503));
  const user = userEvent.setup(); const view = open();
  await user.click(screen.getByRole('button', { name: 'Try the demo film' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Could not open the demo');
  const request = vi.mocked(openDemo).mock.calls[0]![0]; view.unmount();
  open(); await user.click(screen.getByRole('button', { name: 'Try the demo film' }));
  expect(await screen.findByRole('heading', { name: project.name })).toBeInTheDocument();
  expect(openDemo).toHaveBeenLastCalledWith(request);
});

it('storage write failure prevents launching an unrecoverable request', async () => {
  const write = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Browser storage unavailable'); });
  const user = userEvent.setup(); open(); await user.click(screen.getByRole('button', { name: 'Try the demo film' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Browser storage unavailable');
  expect(openDemo).not.toHaveBeenCalled(); write.mockRestore();
});

it('ordinary projects with even the demo title have no reset or demo identity', async () => {
  vi.mocked(getProject).mockResolvedValue({ ...project, name: 'Between the Line — Continuity Study', demo: null });
  open(`/projects/${project.id}`);
  expect(await screen.findByRole('heading', { name: 'Between the Line — Continuity Study' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Reset demo' })).not.toBeInTheDocument();
  expect(screen.queryByText('Demo project')).not.toBeInTheDocument();
});

it('retired history copy is identified and has no reset control', async () => {
  vi.mocked(getProject).mockResolvedValue({ ...project, demo: { ...project.demo!, retired: true } });
  open(`/projects/${project.id}`);
  expect(await screen.findByText('Previous demo copy')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Reset demo' })).not.toBeInTheDocument();
});

it('reset requires explicit confirmation and cancel restores keyboard focus', async () => {
  const user = userEvent.setup(); open(`/projects/${project.id}`);
  const button = await screen.findByRole('button', { name: 'Reset demo' });
  button.focus(); await user.keyboard('{Enter}');
  const dialog = await screen.findByRole('dialog');
  expect(dialog).toHaveTextContent('Previous analysis and action history stay with the old copy');
  expect(resetDemo).not.toHaveBeenCalled();
  await user.click(within(dialog).getByRole('button', { name: 'Keep my changes' }));
  await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  await waitFor(() => expect(button).toHaveFocus());
  expect(resetDemo).not.toHaveBeenCalled();
});

it('reset remains pending until the fresh API project is ready and prevents duplicate requests', async () => {
  let resolve!: (result: Project) => void;
  vi.mocked(resetDemo).mockReturnValue(new Promise(done => { resolve = done; }));
  const user = userEvent.setup(); open(`/projects/${project.id}`);
  await user.click(await screen.findByRole('button', { name: 'Reset demo' }));
  await user.dblClick(await screen.findByRole('button', { name: 'Reset this copy' }));
  expect(screen.getByText('Restoring the authored demo…')).toHaveAttribute('role', 'status');
  expect(resetDemo).toHaveBeenCalledTimes(1);
  await user.keyboard('{Escape}'); expect(screen.getByRole('dialog')).toBeInTheDocument();
  await act(async () => resolve(fresh));
  expect(await screen.findByRole('heading', { name: fresh.name })).toBeInTheDocument();
});

it('reset failure preserves the current workspace and retry targets the same source copy', async () => {
  vi.mocked(resetDemo).mockRejectedValueOnce(new ApiError(503));
  const user = userEvent.setup(); open(`/projects/${project.id}`);
  await user.click(await screen.findByRole('button', { name: 'Reset demo' }));
  await user.click(await screen.findByRole('button', { name: 'Reset this copy' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Demo reset failed');
  await user.click(await screen.findByRole('button', { name: 'Reset this copy' }));
  expect(await screen.findByRole('heading', { name: fresh.name })).toBeInTheDocument();
  expect(vi.mocked(resetDemo).mock.calls).toEqual([[project.id], [project.id]]);
});
