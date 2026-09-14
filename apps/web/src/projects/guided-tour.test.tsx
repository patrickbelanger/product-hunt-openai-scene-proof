import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as api from '@sceneproof/api-client';
import { App } from '../App';
import { Providers } from '../providers';
import { GuidedTour, TOUR_PREFERENCE_KEY } from './GuidedTour';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  getFilmIntelligence: vi.fn().mockResolvedValue({ source: null, runs: [], segments: [], confirmedAnchors: [] }),
  getProject: vi.fn(), listShots: vi.fn(), listFindings: vi.fn(), listReferences: vi.fn(),
  createProject: vi.fn(), updateProjectRules: vi.fn(), uploadReference: vi.fn(), updateReference: vi.fn(), archiveReference: vi.fn(),
  uploadShot: vi.fn(), createAnalysis: vi.fn(), createFindingAction: vi.fn(),
}));

const project = { id: 'c2bca20c-5964-4a4f-a4f5-c11659823f08', name: 'Tour sequence', description: '', rules: 'Keep the coat.', createdAt: '2026-09-13T16:00:00Z', updatedAt: '2026-09-13T16:00:00Z' };
const headings = ['Reference Bible', 'Media and timeline', 'Findings and evidence', 'Resolve and steer'];
const dialog = () => within(screen.getByRole('dialog'));

function open(workspace = false) {
  return render(<Providers>{workspace ? <MemoryRouter initialEntries={[`/projects/${project.id}`]}><App /></MemoryRouter> : <><GuidedTour /><section data-tour="reference-bible">Bible</section><section data-tour="media">Media</section><section data-tour="findings">Findings</section></>}</Providers>);
}

async function complete(user: ReturnType<typeof userEvent.setup>) {
  for (let index = 0; index < 3; index++) await user.click(dialog().getByRole('button', { name: 'Next' }));
  await user.click(dialog().getByRole('button', { name: 'Done' }));
}

beforeEach(() => {
  localStorage.clear();
  vi.mocked(api.getProject).mockResolvedValue(project);
  vi.mocked(api.listShots).mockResolvedValue([]);
  vi.mocked(api.listFindings).mockResolvedValue([]);
  vi.mocked(api.listReferences).mockResolvedValue([]);
});

afterEach(() => { vi.restoreAllMocks(); localStorage.clear(); });

describe('optional guided tour', () => {
  it('offers an unobtrusive invitation on the first actual workspace visit without opening a dialog', async () => {
    open(true);
    expect(await screen.findByRole('complementary', { name: 'Quick tour invitation' })).toHaveTextContent('Take the quick tour');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(localStorage.getItem(TOUR_PREFERENCE_KEY)).toBeNull();
  });

  it('starts at step one with deterministic heading focus and no Back control', async () => {
    const user = userEvent.setup(); open();
    await user.click(screen.getByRole('button', { name: 'Start tour' }));
    await waitFor(() => expect(dialog().getByRole('heading', { name: 'Step 1 of 4 Reference Bible' })).toHaveFocus());
    expect(dialog().queryByRole('button', { name: 'Back' })).not.toBeInTheDocument();
    expect(screen.getByRole('dialog')).toHaveAttribute('aria-modal', 'true');
  });

  it('persists invitation Skip and offers restart after remount', async () => {
    const user = userEvent.setup(); const view = open();
    await user.click(screen.getByRole('button', { name: 'Skip' }));
    expect(localStorage.getItem(TOUR_PREFERENCE_KEY)).toBe('skipped');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Quick tour' })).toHaveFocus());
    view.unmount(); open();
    expect(screen.queryByText('Take the quick tour')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quick tour' }));
    expect(dialog().getByRole('heading')).toHaveTextContent('Step 1 of 4');
  });

  it('has exactly four ordered steps, Back boundaries, Done and completion persistence', async () => {
    const user = userEvent.setup(); open();
    await user.click(screen.getByRole('button', { name: 'Start tour' }));
    for (const [index, heading] of headings.entries()) {
      await waitFor(() => expect(dialog().getByRole('heading', { name: `Step ${index + 1} of 4 ${heading}` })).toHaveFocus());
      if (index < 3) await user.click(dialog().getByRole('button', { name: 'Next' }));
    }
    expect(dialog().queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
    for (let index = 2; index >= 0; index--) {
      await user.click(dialog().getByRole('button', { name: 'Back' }));
      await waitFor(() => expect(dialog().getByRole('heading')).toHaveFocus());
      expect(dialog().getByRole('heading')).toHaveTextContent(headings[index]!);
    }
    expect(dialog().queryByRole('button', { name: 'Back' })).not.toBeInTheDocument();
    await complete(user);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(localStorage.getItem(TOUR_PREFERENCE_KEY)).toBe('completed');
    await waitFor(() => expect(screen.getByRole('button', { name: 'Quick tour' })).toHaveFocus());
  });

  it('does not invite after completion on reload and always permits restart', async () => {
    const user = userEvent.setup(); const view = open();
    await user.click(screen.getByRole('button', { name: 'Start tour' })); await complete(user);
    view.unmount(); open();
    expect(screen.queryByText('Take the quick tour')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quick tour' }));
    expect(dialog().getByRole('heading')).toHaveTextContent('Step 1 of 4');
  });

  it.each([0, 1, 2, 3])('keeps Skip available at step %i and restores restart focus', async index => {
    localStorage.setItem(TOUR_PREFERENCE_KEY, 'completed');
    const user = userEvent.setup(); open();
    await user.click(screen.getByRole('button', { name: 'Quick tour' }));
    for (let position = 0; position < index; position++) await user.click(dialog().getByRole('button', { name: 'Next' }));
    await user.click(dialog().getByRole('button', { name: 'Skip' }));
    expect(localStorage.getItem(TOUR_PREFERENCE_KEY)).toBe('skipped');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Quick tour' })).toHaveFocus());
  });

  it('supports keyboard navigation and Escape from any step', async () => {
    const user = userEvent.setup(); open();
    screen.getByRole('button', { name: 'Quick tour' }).focus();
    await user.keyboard('{Enter}');
    await waitFor(() => expect(dialog().getByRole('heading')).toHaveFocus());
    await user.tab(); expect(dialog().getByRole('button', { name: 'Skip' })).toHaveFocus();
    await user.tab(); expect(dialog().getByRole('button', { name: 'Next' })).toHaveFocus();
    await user.keyboard('{Enter}');
    await waitFor(() => expect(dialog().getByRole('heading')).toHaveFocus());
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await waitFor(() => expect(screen.getByRole('button', { name: 'Quick tour' })).toHaveFocus());
    expect(localStorage.getItem(TOUR_PREFERENCE_KEY)).toBe('skipped');
  });

  it('remains manually usable when storage reads and writes fail', async () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('Unavailable'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Unavailable'); });
    const user = userEvent.setup(); open();
    expect(screen.queryByText('Take the quick tour')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quick tour' })); await complete(user);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Quick tour' }));
    await user.click(dialog().getByRole('button', { name: 'Skip' }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('dismisses the invitation in memory when storage cannot save', async () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Quota'); });
    const user = userEvent.setup(); open();
    await user.click(screen.getByRole('button', { name: 'Skip' }));
    expect(screen.queryByText('Take the quick tour')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Quick tour' })).toBeEnabled();
  });

  it('uses truthful fallback for missing or hidden targets and still completes', async () => {
    const user = userEvent.setup(); open();
    document.querySelector('[data-tour="reference-bible"]')!.removeAttribute('data-tour');
    await user.click(screen.getByRole('button', { name: 'Start tour' }));
    expect(dialog().getByText(/This area is not visible/)).toBeInTheDocument();
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    expect(dialog().getByText(/This area is not visible/)).toBeInTheDocument();
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    await user.click(dialog().getByRole('button', { name: 'Done' }));
    expect(document.querySelector('[data-tour-active]')).toBeNull();
  });

  it('highlights semantic targets, scrolls only on navigation and cleans up', async () => {
    const user = userEvent.setup(); open();
    const target = document.querySelector<HTMLElement>('[data-tour="reference-bible"]')!;
    vi.spyOn(target, 'getClientRects').mockReturnValue([{ top: 900 }] as unknown as DOMRectList);
    vi.spyOn(target, 'getBoundingClientRect').mockReturnValue({ top: 900 } as DOMRect);
    target.scrollIntoView = vi.fn();
    await user.click(screen.getByRole('button', { name: 'Start tour' }));
    expect(target).toHaveAttribute('data-tour-active', '1');
    expect(target.scrollIntoView).toHaveBeenCalledExactlyOnceWith({ block: 'start', behavior: 'instant' });
    fireEvent.scroll(window); fireEvent.resize(window);
    expect(target.scrollIntoView).toHaveBeenCalledTimes(1);
    target.style.display = 'none';
    vi.mocked(target.getClientRects).mockReturnValue([] as unknown as DOMRectList);
    fireEvent.resize(window);
    expect(dialog().getByText(/This area is not visible/)).toBeInTheDocument();
    await user.click(dialog().getByRole('button', { name: 'Skip' }));
    expect(target).not.toHaveAttribute('data-tour-active');
  });

  it('navigates a real empty workspace without mutations, provider calls, request IDs or draft changes', async () => {
    const user = userEvent.setup(); open(true);
    await screen.findByRole('heading', { name: 'No saved findings here.' });
    await user.clear(screen.getByLabelText('Continuity rules', { exact: true }));
    await user.type(screen.getByLabelText('Continuity rules', { exact: true }), 'Unsaved creator draft');
    const requestId = vi.spyOn(crypto, 'randomUUID');
    const fetch = vi.spyOn(globalThis, 'fetch');
    await user.click(screen.getByRole('button', { name: 'Start tour' }));
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    expect(dialog().getByText(/After an analysis, any saved concerns appear here/)).toBeInTheDocument();
    await user.click(dialog().getByRole('button', { name: 'Next' }));
    expect(dialog().getByText(/It does not force Astra to agree/)).toBeInTheDocument();
    await user.click(dialog().getByRole('button', { name: 'Done' }));
    expect(screen.getByLabelText('Continuity rules', { exact: true })).toHaveValue('Unsaved creator draft');
    for (const operation of [api.createProject, api.updateProjectRules, api.uploadReference, api.updateReference, api.archiveReference, api.uploadShot, api.createAnalysis, api.createFindingAction]) expect(operation).not.toHaveBeenCalled();
    expect(fetch).not.toHaveBeenCalled(); expect(requestId).not.toHaveBeenCalled();
    expect(localStorage.length).toBe(1);
  });
});
