import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, expect, it, vi } from 'vitest';
import { ApiError, listShots, uploadShot, type Shot } from '@sceneproof/api-client';
import { Providers } from '../providers';
import { MediaWorkspace } from './MediaWorkspace';

vi.mock('@sceneproof/api-client', async importOriginal => ({ ...await importOriginal<typeof import('@sceneproof/api-client')>(), listShots: vi.fn(), uploadShot: vi.fn() }));
const shot: Shot = { id: 'shot', position: 0, name: 'portrait.png', kind: 'IMAGE', status: 'READY', failureCode: null, durationMs: null, frames: [{ id: 'frame', position: 0, timestampMs: null, width: 40, height: 30, url: '/frame.png' }] };
beforeEach(() => { vi.mocked(listShots).mockResolvedValue([]); });

it('imports and selects persisted frames without inventing still timestamps', async () => {
  vi.mocked(uploadShot).mockImplementation(async () => { vi.mocked(listShots).mockResolvedValue([shot]); return shot; });
  const user = userEvent.setup();
  render(<Providers><MediaWorkspace projectId="project" /></Providers>);
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  const file = new File(['fixture'], 'portrait.png', { type: 'image/png' });
  await user.upload(input, file);
  await user.click(screen.getByRole('button', { name: 'Import shot' }));
  expect(await screen.findByRole('img', { name: 'Frame from portrait.png' })).toHaveAttribute('src', '/frame.png');
  expect(screen.getByText('portrait.png · Still image')).toBeVisible();
  expect(screen.getByText('Playhead · Still image')).toBeVisible();
  expect(uploadShot).toHaveBeenCalledWith('project', file);
});

it('preserves file after failure and supports an explicit retry', async () => {
  vi.mocked(uploadShot).mockRejectedValueOnce(new ApiError(422, { type: 'about:blank', title: 'Invalid image', status: 422, detail: 'The image is corrupt.' })).mockResolvedValueOnce(shot);
  const user = userEvent.setup();
  render(<Providers><MediaWorkspace projectId="project" /></Providers>);
  await user.upload(document.querySelector('input[type="file"]') as HTMLInputElement, new File(['bad'], 'bad.png', { type: 'image/png' }));
  await user.click(screen.getByRole('button', { name: 'Import shot' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('The image is corrupt.');
  await user.click(screen.getByRole('button', { name: 'Import shot' }));
  await waitFor(() => expect(uploadShot).toHaveBeenCalledTimes(2));
});
