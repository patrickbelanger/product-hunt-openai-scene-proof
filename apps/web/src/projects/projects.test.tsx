import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createProject, getProject, listProjects, listShots } from '@sceneproof/api-client';
import { App } from '../App';
import { Providers } from '../providers';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  createProject: vi.fn(), getProject: vi.fn(), listProjects: vi.fn(), listShots: vi.fn(),
}));

const project = {
  id: 'c2bca20c-5964-4a4f-a4f5-c11659823f08', name: 'Between the Line',
  description: 'Metro sequence', rules: 'Keep the black blazer.',
  createdAt: '2026-09-12T16:00:00Z', updatedAt: '2026-09-12T16:00:00Z',
};

function open(path: string) {
  return render(<Providers><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></Providers>);
}

beforeEach(() => {
  vi.mocked(listShots).mockResolvedValue([]);
  vi.mocked(listProjects).mockResolvedValue({ items: [], page: 0, hasNext: false });
  vi.mocked(createProject).mockResolvedValue(project);
  vi.mocked(getProject).mockResolvedValue(project);
});

describe('project critical path', () => {
  it('creates a project with rules and opens the saved workspace', async () => {
    const user = userEvent.setup();
    open('/');
    await user.click(screen.getByRole('link', { name: /create a project/i }));
    await user.type(screen.getByLabelText(/project name/i), '  Between the Line  ');
    await user.type(screen.getByLabelText('Description'), project.description);
    await user.type(screen.getByLabelText('Continuity rules'), project.rules);
    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(await screen.findByRole('heading', { name: project.name })).toBeInTheDocument();
    expect(createProject).toHaveBeenCalledWith(expect.objectContaining({ name: project.name, rules: project.rules }), expect.anything());
    expect(screen.getByText(project.rules)).toBeInTheDocument();
    expect(screen.getByText('Nothing reviewed yet.')).toBeInTheDocument();
  });

  it('preserves entered details after save failure and allows retry', async () => {
    vi.mocked(createProject).mockRejectedValueOnce(new ApiError(503));
    const user = userEvent.setup();
    open('/projects/new');
    await user.type(screen.getByLabelText(/project name/i), project.name);
    await user.type(screen.getByLabelText('Continuity rules'), project.rules);
    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Project not saved');
    expect(screen.getByLabelText('Continuity rules')).toHaveValue(project.rules);
    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(await screen.findByRole('heading', { name: project.name })).toBeInTheDocument();
  });

  it('rejects whitespace names before calling the API', async () => {
    const user = userEvent.setup();
    open('/projects/new');
    await user.type(screen.getByLabelText(/project name/i), '   ');
    await user.click(screen.getByRole('button', { name: 'Create project' }));
    expect(screen.getByText('Give your project a name.')).toBeInTheDocument();
    expect(createProject).not.toHaveBeenCalled();
  });

  it('shows a missing project state with a route back to projects', async () => {
    vi.mocked(getProject).mockRejectedValue(new ApiError(404));
    open(`/projects/${project.id}`);
    expect(await screen.findByRole('alert')).toHaveTextContent('Project not found');
    expect(screen.getByRole('link', { name: 'All projects' })).toHaveAttribute('href', '/');
  });
});
