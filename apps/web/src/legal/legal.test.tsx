import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, expect, it, vi } from 'vitest';
import * as api from '@sceneproof/api-client';
import { App } from '../App';
import { Providers } from '../providers';
import { repositoryUrl, validateProductionDisclosures } from './disclosures';

vi.mock('@sceneproof/api-client', async importOriginal => ({
  ...await importOriginal<typeof import('@sceneproof/api-client')>(),
  listProjects: vi.fn(), getProject: vi.fn(), listReferences: vi.fn(), listShots: vi.fn(), listFindings: vi.fn(), getFilmIntelligence: vi.fn(),
  createAnalysis: vi.fn(), understandFilm: vi.fn(), createFindingAction: vi.fn(), uploadSourceFilm: vi.fn(), uploadShot: vi.fn(), uploadReference: vi.fn(),
}));

function open(path: string) {
  return render(<Providers><MemoryRouter initialEntries={[path]}><App /></MemoryRouter></Providers>);
}

beforeEach(() => {
  for (const key of ['VITE_LEGAL_OPERATOR', 'VITE_PRIVACY_CONTACT', 'VITE_PRIVACY_CONTACT_EMAIL', 'VITE_PRIVACY_HOSTING', 'VITE_PRIVACY_RETENTION', 'VITE_PRIVACY_TRANSFERS', 'VITE_PRIVACY_LEGAL_BASIS']) vi.stubEnv(key, '');
  localStorage.clear();
  vi.mocked(api.listProjects).mockResolvedValue({ items: [], page: 0, hasNext: false });
  vi.mocked(api.getProject).mockResolvedValue({ id: 'project', name: 'Disclosure test', description: '', rules: '', createdAt: '2026-09-17T00:00:00Z', updatedAt: '2026-09-17T00:00:00Z', demo: null });
  vi.mocked(api.listReferences).mockResolvedValue([]);
  vi.mocked(api.listShots).mockResolvedValue([]);
  vi.mocked(api.listFindings).mockResolvedValue([]);
  vi.mocked(api.getFilmIntelligence).mockResolvedValue({ source: null, runs: [], segments: [], confirmedAnchors: [] });
});

afterEach(() => { vi.unstubAllEnvs(); vi.restoreAllMocks(); });

it.each([['/privacy', 'Privacy Policy'], ['/terms', 'Terms of Use']])('renders %s directly with accessible footer and no data/provider requests', async (path, title) => {
  const request = vi.spyOn(globalThis, 'fetch');
  open(path);
  expect(screen.getByRole('heading', { name: title, level: 1 })).toBeVisible();
  expect(screen.getByText(/Last updated:/)).toHaveTextContent('September 17, 2026');
  expect(document.title).toBe(`${title} — SceneProof`);
  expect(document.body).not.toHaveTextContent(/\bTODO\b|disclosure draft|remain to be confirmed|need operator confirmation/i);
  const footer = within(screen.getByRole('navigation', { name: 'Legal and source links' }));
  expect(footer.getByRole('link', { name: 'Privacy' })).toHaveAttribute('href', '/privacy');
  expect(footer.getByRole('link', { name: 'Terms' })).toHaveAttribute('href', '/terms');
  expect(footer.getByRole('link', { name: 'GitHub' })).toHaveAttribute('href', repositoryUrl);
  expect(request).not.toHaveBeenCalled();
  for (const operation of [api.listProjects, api.getProject, api.createAnalysis, api.understandFilm, api.createFindingAction]) expect(operation).not.toHaveBeenCalled();
});

it('uses only a local-development fallback without configuration and discloses retention and shared access', () => {
  open('/privacy');
  expect(screen.getByText(/Privacy contact is unavailable in this local development instance/)).toBeVisible();
  expect(screen.queryByRole('link', { name: /@/ })).not.toBeInTheDocument();
  expect(screen.getByText(/Reset Demo is not deletion/)).toBeVisible();
  expect(screen.getByText(/Ordinary projects are retained until you delete them/)).toHaveTextContent('database project is already deleted');
  expect(screen.getByText(/deletion-request\/media-cleanup timestamps/)).toBeVisible();
  expect(screen.getByText(/Ordinary project deletion is disabled for demo copies/)).toHaveTextContent('no automatic cleanup');
  expect(screen.queryByText(/Reservations older than 24 hours|pruned during/)).not.toBeInTheDocument();
  expect(screen.getByText(/no private user accounts or per-user project access isolation/)).toBeVisible();
  expect(screen.getByText(/outside Québec or Canada/)).toBeVisible();
  expect(screen.getByText(/including entered explanation and scope/)).toBeVisible();
});

it('renders confirmed contact configuration as plain text and a private email link', () => {
  vi.stubEnv('VITE_LEGAL_OPERATOR', 'Test operator');
  vi.stubEnv('VITE_PRIVACY_CONTACT', 'Test privacy officer');
  vi.stubEnv('VITE_PRIVACY_CONTACT_EMAIL', 'privacy@example.test');
  open('/privacy');
  expect(screen.getByText('Test operator')).toBeVisible();
  expect(screen.getByText('Test privacy officer')).toBeVisible();
  expect(screen.getByRole('link', { name: 'privacy@example.test' })).toHaveAttribute('href', 'mailto:privacy%40example.test');
});

it('does not turn malformed contact configuration into an actionable mail link', () => {
  vi.stubEnv('VITE_PRIVACY_CONTACT_EMAIL', 'privacy@example.test?body=unexpected');
  open('/privacy');
  expect(screen.getByText(/Privacy contact is unavailable in this local development instance/)).toBeVisible();
  expect(screen.queryByRole('link', { name: /@/ })).not.toBeInTheDocument();
});

it.each(['VITE_LEGAL_OPERATOR', 'VITE_PRIVACY_CONTACT_EMAIL'] as const)('rejects production without %s', key => {
  const environment = { VITE_LEGAL_OPERATOR: 'Test operator', VITE_PRIVACY_CONTACT_EMAIL: 'privacy@example.test', [key]: ' ' };
  expect(() => validateProductionDisclosures(environment)).toThrow(key);
});

it('rejects invalid email and unfinished optional configuration without echoing values', () => {
  expect(() => validateProductionDisclosures({ VITE_LEGAL_OPERATOR: 'Test operator', VITE_PRIVACY_CONTACT_EMAIL: 'invalid?secret' })).toThrow('VITE_PRIVACY_CONTACT_EMAIL');
  expect(() => validateProductionDisclosures({ VITE_LEGAL_OPERATOR: 'Test operator', VITE_PRIVACY_CONTACT_EMAIL: 'privacy@example.test', VITE_PRIVACY_RETENTION: 'TODO: confirm internal details' })).toThrow('VITE_PRIVACY_RETENTION');
  expect(() => validateProductionDisclosures({ VITE_LEGAL_OPERATOR: 'Test operator', VITE_PRIVACY_CONTACT_EMAIL: 'privacy@example.test' })).not.toThrow();
});

it.each(['/privacy', '/terms'])('fails closed at runtime on %s if a production bundle lacks required configuration', path => {
  vi.stubEnv('PROD', true);
  expect(() => open(path)).toThrow('Legal disclosure configuration required: VITE_LEGAL_OPERATOR, VITE_PRIVACY_CONTACT_EMAIL');
});

it.each(['/privacy', '/terms'])('renders finished production copy on %s using supplied identity and mailbox', path => {
  vi.stubEnv('PROD', true);
  vi.stubEnv('VITE_LEGAL_OPERATOR', 'Test operator');
  vi.stubEnv('VITE_PRIVACY_CONTACT_EMAIL', 'privacy@example.test');
  open(path);
  expect(screen.getByRole('link', { name: 'privacy@example.test' })).toBeVisible();
  expect(document.body).not.toHaveTextContent(/\bTODO\b|local development|disclosure draft|remain to be confirmed/i);
});

it('links from the library to both legal pages and back using keyboard-accessible links', async () => {
  const user = userEvent.setup();
  open('/');
  const privacy = screen.getByRole('link', { name: 'Privacy' });
  privacy.focus();
  await user.keyboard('{Enter}');
  expect(screen.getByRole('heading', { name: 'Privacy Policy' })).toBeVisible();
  await user.click(screen.getByRole('link', { name: 'Terms' }));
  expect(screen.getByRole('heading', { name: 'Terms of Use' })).toBeVisible();
  await user.click(screen.getByRole('link', { name: /Back to projects/ }));
  expect(screen.getByRole('button', { name: 'Try the demo film' })).toBeVisible();
});

it('shows one notice at each relevant upload form without uploading or starting analysis', async () => {
  const user = userEvent.setup();
  open('/projects/project');
  const film = await screen.findByRole('region', { name: 'Film Intelligence' });
  expect(await within(film).findByText(/Only upload media you have the right to process/)).toBeVisible();
  await user.click(screen.getByRole('tab', { name: 'Continuity Findings' }));
  const media = screen.getByRole('region', { name: 'Media workspace' });
  expect(within(media).getByText(/Only upload media you have the right to process/)).toBeVisible();
  await user.click(screen.getByRole('button', { name: /Add visual reference/ }));
  const dialog = await screen.findByRole('dialog', { name: 'Add visual reference' });
  expect(within(dialog).getAllByText(/Only upload media you have the right to process/)).toHaveLength(1);
  expect(within(dialog).getByRole('link', { name: 'Privacy details (opens a new tab)' })).toHaveAttribute('target', '_blank');
  for (const operation of [api.createAnalysis, api.understandFilm, api.createFindingAction, api.uploadSourceFilm, api.uploadShot, api.uploadReference]) expect(operation).not.toHaveBeenCalled();
});
