import { useEffect, useState } from 'react';
import { Group, Paper, Text } from '@mantine/core';
import type { FilmRun } from '@sceneproof/api-client';

export const stageLabels: Record<FilmRun['stage'], string> = {
  PREPARING_SOURCE: 'Verifying source film', DETECTING_STRUCTURE: 'Preparing visual structure',
  TRANSCRIBING_AUDIO: 'Extracting and transcribing audio', UNDERSTANDING_FILM: 'Understanding visual and narrative context',
  BUILDING_CANDIDATES: 'Validating and saving candidates', SUCCEEDED: 'Film Understanding complete', FAILED: 'Film Understanding failed',
};
const workflow: FilmRun['stage'][] = ['PREPARING_SOURCE', 'DETECTING_STRUCTURE', 'TRANSCRIBING_AUDIO', 'UNDERSTANDING_FILM', 'BUILDING_CANDIDATES'];
export const isActive = (run?: FilmRun) => !!run && run.stage !== 'SUCCEEDED' && run.stage !== 'FAILED';

export function FilmProgress({ run, fetching, updatedAt, disconnected }: { run: FilmRun; fetching: boolean; updatedAt: number; disconnected: boolean }) {
  const [now, setNow] = useState(Date.now);
  const active = isActive(run);
  useEffect(() => {
    if (!active) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [active]);
  const recorded = run.stages.filter(stage => workflow.includes(stage.stage));
  const current = run.stage === 'FAILED' ? recorded.at(-1)?.stage : run.stage;
  const step = run.stage === 'SUCCEEDED' ? workflow.length : Math.max(1, workflow.indexOf(current!) + 1);
  const elapsed = Math.max(0, Math.floor(((run.completedAt ? Date.parse(run.completedAt) : now) - Date.parse(run.startedAt)) / 1000));
  const lastStageTime = run.completedAt ?? recorded.at(-1)?.startedAt ?? run.startedAt;
  const age = Math.max(0, Math.floor((now - updatedAt) / 1000));
  return <Paper withBorder p="md" className="film-progress">
    <Group justify="space-between"><Text fw={600}>{active ? 'Film Understanding in progress' : stageLabels[run.stage]}</Text><Text size="sm">Step {step} of {workflow.length}</Text></Group>
    {active && <Text role="status" size="sm" mt="xs">{stageLabels[run.stage]}</Text>}
    <ol className="workflow-progress" aria-label="Film Understanding workflow progress">
      {workflow.map(stage => {
        const saved = recorded.find(value => value.stage === stage);
        const state = stage === current && run.stage === 'FAILED' ? 'failed' : stage === current && active ? 'active' : saved?.completedAt ? 'complete' : 'future';
        return <li key={stage} data-state={state} aria-current={state === 'active' || state === 'failed' ? 'step' : undefined}>
          <span className="workflow-track" aria-hidden="true" /><span className="workflow-icon" aria-hidden="true">{state === 'complete' ? '✓' : state === 'failed' ? '!' : state === 'active' ? '●' : '○'}</span>
          <span>{stageLabels[stage]}<small>{state === 'complete' ? 'Completed' : state === 'failed' ? 'Failed' : state === 'active' ? 'In progress' : 'Not started'}</small></span>
        </li>;
      })}
    </ol>
    <Group gap="md" className="film-run-meta"><Text size="xs">Elapsed: {elapsed}s</Text><Text size="xs">Started <time dateTime={run.startedAt}>{new Date(run.startedAt).toLocaleTimeString()}</time></Text><Text size="xs">Last stage update <time dateTime={lastStageTime}>{new Date(lastStageTime).toLocaleTimeString()}</time></Text><Text size="xs" className="film-live-state">{disconnected ? 'Connection lost · retrying reads' : active ? fetching ? 'Syncing…' : `Live · updated ${age < 1 ? 'just now' : `${age}s ago`}` : 'Saved · live updates stopped'}</Text></Group>
  </Paper>;
}
