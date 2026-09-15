import { Text } from '@mantine/core';
import type { FilmIntelligence } from '@sceneproof/api-client';
import manifest from '../../../../demo/runtime/manifest.json';

export function SourceProvenance({ source }: { source: NonNullable<FilmIntelligence['source']> }) {
  const derived = source.sha256 === manifest.sourceFilm.sha256;
  const master = source.sha256 === manifest.sourceProvenance.masterSha256;
  return <div className="source-provenance"><Text size="sm" fw={600}>Analysis source · {(source.durationMs / 1000).toFixed(2)}s</Text>
    <Text size="sm">{source.name}</Text><Text size="xs" c="dimmed">Source fixed for this project.</Text>
    <details><summary>Source details</summary>
      {derived && <Text size="xs">Master · {(manifest.sourceProvenance.masterDurationUs / 1000000).toFixed(2)}s · Analysis range · {(manifest.sourceProvenance.sourceStartUs / 1000000).toFixed(2)}–{(manifest.sourceProvenance.sourceEndUs / 1000000).toFixed(2)}s</Text>}
      {master && <Text size="xs">Master source · historical full-duration source</Text>}
      <Text size="xs">Source identity: {source.id}</Text>
      <Text size="xs" mt="xs">Source replacement is not available for this project. Create a new project to analyze a different film.</Text>
    </details></div>;
}
