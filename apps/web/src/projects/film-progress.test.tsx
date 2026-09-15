import { act, render, screen, within } from '@testing-library/react';
import { MantineProvider } from '@mantine/core';
import { afterEach, expect, it, vi } from 'vitest';
import type { FilmRun } from '@sceneproof/api-client';
import { FilmProgress, stageLabels } from './FilmProgress';

const startedAt = '2026-09-14T12:00:00Z';
const stages: FilmRun['stage'][] = ['PREPARING_SOURCE', 'DETECTING_STRUCTURE', 'TRANSCRIBING_AUDIO', 'UNDERSTANDING_FILM', 'BUILDING_CANDIDATES'];
const run = { stage: 'UNDERSTANDING_FILM', startedAt, completedAt: null, stages: stages.slice(0, 4).map((stage, index) => ({ stage, startedAt, completedAt: index < 3 ? startedAt : null })) } as FilmRun;
function view(value = run) { return <MantineProvider><FilmProgress run={value} fetching={false} updatedAt={Date.parse(startedAt)} disconnected={false} /></MantineProvider>; }
afterEach(() => vi.useRealTimers());

it('shows five workflow tracks with recorded completion, current activity and future state; elapsed is not an ETA', () => {
  vi.useFakeTimers(); vi.setSystemTime(new Date('2026-09-14T12:00:22Z'));
  render(view());
  const progress = screen.getByRole('list', { name: 'Film Understanding workflow progress' });
  const items = within(progress).getAllByRole('listitem');
  expect(items.map(item => item.dataset.state)).toEqual(['complete', 'complete', 'complete', 'active', 'future']);
  expect(items[3]).toHaveAttribute('aria-current', 'step');
  expect(screen.getByText('Step 4 of 5')).toBeVisible();
  expect(screen.getByText('Elapsed: 22s')).toBeVisible();
  act(() => vi.advanceTimersByTime(1000));
  expect(screen.getByText('Elapsed: 23s')).toBeVisible();
  expect(screen.queryByText(/estimated remaining|\d+%/i)).not.toBeInTheDocument();
});

it('stops at the actual failed stage even though the backend closes its timestamp', () => {
  render(view({ ...run, stage: 'FAILED', completedAt: '2026-09-14T12:00:25Z', stages: [...run.stages.map(stage => ({ ...stage, completedAt: startedAt })), { stage: 'FAILED', startedAt, completedAt: startedAt }] }));
  expect(screen.getAllByRole('listitem').map(item => item.dataset.state)).toEqual(['complete', 'complete', 'complete', 'failed', 'future']);
  expect(screen.getByText('Elapsed: 25s')).toBeVisible();
  expect(screen.getByText('Saved · live updates stopped')).toBeVisible();
});

it('shows all persisted completed stages without active animation after success', () => {
  render(view({ ...run, stage: 'SUCCEEDED', completedAt: startedAt, stages: stages.map(stage => ({ stage, startedAt, completedAt: startedAt })) }));
  expect(screen.getByText('Step 5 of 5')).toBeVisible();
  for (const item of screen.getAllByRole('listitem')) expect(item).toHaveAttribute('data-state', 'complete');
});

it('does not invent completed stages when historical data is missing', () => {
  render(view({ ...run, stages: [run.stages[3]!] }));
  expect(screen.getAllByRole('listitem')[0]).toHaveTextContent(`${stageLabels.PREPARING_SOURCE}Not started`);
});
