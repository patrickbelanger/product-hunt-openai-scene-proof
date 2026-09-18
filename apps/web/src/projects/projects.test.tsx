import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createProject, getProject, getAnalysis, listProjects, listShots, listFindings, listReferences } from '@sceneproof/api-client';
import { App } from '../App';
import { Providers } from '../providers';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  getFilmIntelligence: vi.fn().mockResolvedValue({ source: null, runs: [], segments: [], confirmedAnchors: [] }),
  createProject: vi.fn(), getProject: vi.fn(), getAnalysis: vi.fn(), listProjects: vi.fn(), listShots: vi.fn(), listFindings: vi.fn(), listReferences: vi.fn(),
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
  vi.mocked(listReferences).mockResolvedValue([]);
  vi.mocked(listFindings).mockResolvedValue([]);
  vi.mocked(listShots).mockResolvedValue([]);
  vi.mocked(listProjects).mockResolvedValue({ items: [], page: 0, hasNext: false });
  vi.mocked(createProject).mockResolvedValue(project);
  vi.mocked(getProject).mockResolvedValue(project);
});

describe('project critical path', () => {
  it('defaults to Film Intelligence and supports keyboard tabs without losing inspection state or drafts', async () => {
    vi.mocked(listShots).mockResolvedValue([{ id: 'shot', name: 'Test clip', position: 0, kind: 'VIDEO', status: 'READY', failureCode: null, durationMs: 4000, frames: [0, 1].map(position => ({ id: `sample-${position}`, position, timestampMs: position * 2000, width: 40, height: 40, url: '/test.png' })) }]);
    const user = userEvent.setup(); open(`/projects/${project.id}`);
    const film = await screen.findByRole('tab', { name: 'Film Intelligence' });
    const findings = screen.getByRole('tab', { name: 'Continuity Findings' });
    expect(film).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText(/Understand the film's visual and narrative context/)).toBeVisible();
    film.focus(); await user.keyboard('{ArrowRight}');
    expect(findings).toHaveFocus(); expect(findings).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText(/Inspect frame-level continuity issues/)).toBeVisible();
    expect(screen.getByText(/Import a shot when you want precise/)).toBeVisible();
    expect(await screen.findByText(/If you have not run continuity analysis yet/)).toBeVisible();
    const rules = screen.getByLabelText('Continuity rules', { exact: true });
    await user.clear(rules); await user.type(rules, 'Unsaved creator draft');
    const timeline = screen.getByRole('region', { name: 'Keyboard timeline inspection' });
    timeline.focus(); await user.keyboard('i{ArrowRight}o');
    expect(screen.getByText('In 0.00s · Out 2.00s · Selection 2.00s')).toBeVisible();
    findings.focus(); await user.keyboard('{ArrowLeft}'); expect(film).toHaveFocus();
    expect(timeline).not.toBeVisible();
    await user.keyboard('{ArrowRight}');
    expect(screen.getByRole('region', { name: 'Keyboard timeline inspection' })).toBe(timeline);
    expect(screen.getByText('Playhead · Clip 2.00s')).toBeVisible();
    expect(screen.getByTestId('inspection-range')).toBeVisible();
    expect(rules).toHaveValue('Unsaved creator draft');
  });

  it('introduces Film Intelligence then reveals mounted findings targets without remounting', async () => {
    const user = userEvent.setup(); open(`/projects/${project.id}`);
    await user.click(await screen.findByRole('button', { name: 'Quick tour' }));
    expect(screen.getByRole('tab', { name: 'Film Intelligence' })).toHaveAttribute('aria-selected', 'true');
    expect(await screen.findByRole('dialog')).toHaveTextContent('Anchors are proposals, not rules.');
    await user.click(screen.getByRole('button', { name: 'Next' }));
    expect(screen.getByRole('tab', { name: 'Continuity Findings' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('Continuity rules', { exact: true })).toBeVisible();
  });

  it('opens a findings deep link in the findings tab', async () => {
    open(`/projects/${project.id}?finding=missing`);
    expect(await screen.findByRole('tab', { name: 'Continuity Findings' })).toHaveAttribute('aria-selected', 'true');
  });

  it('keeps findings visible when clearing its analysis link', async () => {
    vi.mocked(getAnalysis).mockRejectedValue(new ApiError(404));
    const user = userEvent.setup(); open(`/projects/${project.id}?analysisId=missing`);
    await user.click(await screen.findByRole('button', { name: 'All saved findings' }));
    expect(screen.getByRole('tab', { name: 'Continuity Findings' })).toHaveAttribute('aria-selected', 'true');
    expect(await screen.findByText('No continuity analysis has been run yet.')).toBeVisible();
  });

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
    expect(await screen.findByText('No continuity analysis has been run yet.')).toBeInTheDocument();
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
