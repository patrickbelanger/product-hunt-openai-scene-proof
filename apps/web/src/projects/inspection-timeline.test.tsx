import { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MantineProvider } from '@mantine/core';
import { expect, it } from 'vitest';
import type { Shot } from '@sceneproof/api-client';
import { InspectionTimeline } from './InspectionTimeline';

const shots: Shot[] = [0, 1].map(index => ({ id: `shot-${index}`, name: `Segment ${index}`, position: index, kind: 'VIDEO', status: 'READY', failureCode: null, durationMs: 10000, frames: [0, 1, 2].map(position => ({ id: `frame-${index}-${position}`, position, timestampMs: position * 2000, width: 40, height: 40, url: '/test.png' })) }));
function Harness() {
  const [selected, setSelected] = useState('frame-0-0');
  return <MantineProvider><InspectionTimeline shots={shots} segments={shots.map((shot, index) => ({ id: shot.id, position: index, startMs: index * 10000, endMs: (index + 1) * 10000, shot }))} selectedId={selected} onSelect={setSelected}><input aria-label="Editor" /><textarea aria-label="Notes" /><button>Outside shortcuts</button></InspectionTimeline></MantineProvider>;
}

it('supports focused frame, segment and boundary navigation with a visible source playhead', async () => {
  render(<Harness />); const user = userEvent.setup();
  const timeline = screen.getByRole('region', { name: 'Keyboard timeline inspection' }); timeline.focus();
  expect(timeline).toHaveFocus();
  await user.keyboard('{ArrowRight}'); expect(screen.getByText('Playhead · Source 2.00s')).toBeVisible();
  await user.keyboard('{Shift>}{ArrowRight}{/Shift}'); expect(screen.getByText('Playhead · Source 10.00s')).toBeVisible();
  await user.keyboard('{Shift>}{ArrowLeft}{/Shift}'); expect(screen.getByText('Playhead · Source 0.00s')).toBeVisible();
  await user.keyboard('{End}{ArrowLeft}'); expect(screen.getByText('Playhead · Source 12.00s')).toBeVisible();
  await user.keyboard('{Home}'); expect(screen.getByText('Frame 1 of 6')).toBeVisible();
});

it('marks, adjusts, highlights and clears a local inspection range without provider controls', async () => {
  render(<Harness />); const user = userEvent.setup(); screen.getByRole('region').focus();
  await user.keyboard('{ArrowRight}i{Shift>}{ArrowRight}{/Shift}o');
  expect(screen.getByText('In 2.00s · Out 10.00s · Selection 8.00s')).toBeVisible();
  expect(screen.getByTestId('inspection-range')).toBeVisible();
  expect(screen.getByRole('button', { name: 'Go to Mark In 2.00s' })).toBeVisible();
  expect(screen.getByRole('button', { name: 'Go to Mark Out 10.00s' })).toBeVisible();
  await user.keyboard('{ArrowRight}o'); expect(screen.getByText(/Selection 10.00s/)).toBeVisible();
  await user.keyboard('{Escape}'); expect(screen.queryByTestId('inspection-range')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Analyze selection/i })).not.toBeInTheDocument();
});

it('preserves normal input/editor keys and tab does not trap focus', async () => {
  render(<Harness />); const user = userEvent.setup();
  await user.click(screen.getByRole('textbox', { name: 'Editor' })); await user.keyboard('io{ArrowRight}{Escape}');
  expect(screen.getByRole('textbox', { name: 'Editor' })).toHaveValue('io');
  expect(screen.getByText('Frame 1 of 6')).toBeVisible();
  expect(screen.queryByTestId('inspection-range')).not.toBeInTheDocument();
  await user.tab(); expect(screen.getByRole('textbox', { name: 'Notes' })).toHaveFocus();
  fireEvent.keyDown(screen.getByRole('textbox', { name: 'Notes' }), { key: 'ArrowRight', shiftKey: true });
  expect(screen.getByText('Frame 1 of 6')).toBeVisible();
});

it('supports mouse marks and rejects a reversed range without changing it', async () => {
  render(<Harness />); const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Mark Out' }));
  await user.click(screen.getByRole('button', { name: 'Inspect 10.00s' }));
  await user.click(screen.getByRole('button', { name: 'Mark In' }));
  expect(screen.getByRole('status')).toHaveTextContent('Mark In must be before');
  await user.click(screen.getByRole('button', { name: 'Clear range' }));
  expect(screen.getByText('In — · Out — · Selection —')).toBeVisible();
});
