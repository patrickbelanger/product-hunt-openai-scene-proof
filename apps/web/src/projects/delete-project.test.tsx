import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, expect, it, vi } from 'vitest';
import { deleteProject, type Project } from '@sceneproof/api-client';
import { Providers } from '../providers';
import { DeleteProject } from './DeleteProject';

vi.mock('@sceneproof/api-client', async importOriginal => ({ ...await importOriginal<typeof import('@sceneproof/api-client')>(), deleteProject: vi.fn() }));
const project: Project = { id: 'project', name: 'My film', description: '', rules: '', createdAt: '', updatedAt: '', demo: null };
function open(value = project) { render(<Providers><MemoryRouter initialEntries={['/project']}><Routes><Route path="/project" element={<DeleteProject project={value} />} /><Route path="/" element={<h1>Project library</h1>} /></Routes></MemoryRouter></Providers>); }
beforeEach(() => vi.mocked(deleteProject).mockResolvedValue(undefined));

it('names the project, requires deliberate exact confirmation and restores focus after cancel', async () => {
  open(); const user = userEvent.setup(); const opener = screen.getByRole('button', { name: 'Delete project' }); await user.click(opener);
  const dialog = await screen.findByRole('dialog', { name: 'Delete My film?' });
  expect(dialog).toHaveTextContent('irreversible');
  expect(within(dialog).getByRole('button', { name: 'Delete permanently' })).toBeDisabled();
  await user.click(within(dialog).getByRole('button', { name: 'Keep project' }));
  await waitFor(() => expect(opener).toHaveFocus()); expect(deleteProject).not.toHaveBeenCalled();
});

it('deletes once after confirmation and navigates to the library', async () => {
  open(); const user = userEvent.setup(); await user.click(screen.getByRole('button', { name: 'Delete project' }));
  fireEvent.change(await screen.findByLabelText('Type the project name to confirm'), { target: { value: project.name } });
  await user.click(screen.getByRole('button', { name: 'Delete permanently' }));
  expect(await screen.findByRole('heading', { name: 'Project library' })).toBeVisible();
  expect(deleteProject).toHaveBeenCalledExactlyOnceWith(project.id, project.name);
});

it('retains cleanup errors and confirmation for a safe retry', async () => {
  vi.mocked(deleteProject).mockRejectedValueOnce(new Error('Media cleanup pending. Retry deletion.'));
  open(); const user = userEvent.setup(); await user.click(screen.getByRole('button', { name: 'Delete project' }));
  fireEvent.change(await screen.findByLabelText('Type the project name to confirm'), { target: { value: project.name } });
  await user.click(screen.getByRole('button', { name: 'Delete permanently' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Media cleanup pending');
  await user.click(screen.getByRole('button', { name: 'Delete permanently' }));
  expect(await screen.findByRole('heading', { name: 'Project library' })).toBeVisible();
});

it('does not offer deletion for current or retired demo copies', () => {
  open({ ...project, demo: { instanceId: 'demo', templateVersion: 'v1', retired: false } });
  open({ ...project, demo: { instanceId: 'demo', templateVersion: 'v1', retired: true } });
  expect(screen.queryByRole('button', { name: 'Delete project' })).not.toBeInTheDocument();
});
