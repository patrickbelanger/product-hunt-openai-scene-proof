import { Badge, Group, Paper, Text, Title } from '@mantine/core';
import type { FilmRun } from '@sceneproof/api-client';

export function UnderstandingSummary({ result }: { result: NonNullable<FilmRun['result']> }) {
  return <Paper className="understanding-summary" withBorder p="md">
    <Title order={3} size="h4">Understanding summary</Title>
    <Text size="xs" c="dimmed" mt="sm">Observed arc · model interpretation</Text>
    <div className="film-summary-copy">{result.summary.split(/\n+/).filter(Boolean).map((paragraph, index) => <Text key={index} size="sm" mt="xs">{paragraph}</Text>)}</div>
    <Group gap="xs" mt="md" aria-label="Understanding statistics"><Badge variant="light">{result.inspectedFrameIds.length} sampled frames</Badge><Badge variant="light">{result.inspectedSegmentIds.length} analysis segments</Badge><Badge variant="light">{result.entities.length} recurring entities</Badge><Badge color="yellow" variant="light">{result.potentialConcerns.length} potential concerns</Badge></Group>
    {!!result.entities.length && <Text size="sm" mt="sm">Recurring elements: {result.entities.map(entity => entity.name).join(' · ')}</Text>}
    {!!result.narrativeCues.length && <Text size="sm" mt="xs">Narrative context: {result.narrativeCues.map(cue => cue.title).join(' · ')}</Text>}
  </Paper>;
}

export function FilmLimitations({ warnings }: { warnings: string[] }) {
  return <details className="film-limitations"><summary>Methodology & limitations</summary>
    <ul><li>Only sampled frames were inspected. Deterministic analysis segments are not exhaustive shots or scenes; motion and intervening edits may not be observed.</li>
      <li>Transcript timestamps are approximate. Transcript and narrative context are not proof of literal on-screen action.</li>
      <li>Proposals require creator confirmation. Difference alone is not a continuity error.</li></ul>
    {warnings.map((warning, index) => <Text key={index} size="sm" mt="xs">{warning}</Text>)}
  </details>;
}
