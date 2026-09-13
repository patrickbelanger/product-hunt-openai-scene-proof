import { useState } from 'react';
import { Alert, Button, FileInput, Group, Image, Stack, Text, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listShots, uploadShot, type Finding } from '@sceneproof/api-client';
import { comparisonPair, EvidenceComparison } from './EvidenceComparison';

const failureMessages: Record<string, string> = {
  UNSUPPORTED_MEDIA: 'Unsupported or invalid file. Use JPEG, PNG or MP4/H.264.',
  INVALID_IMAGE: 'The image is corrupt or could not be decoded.',
  IMAGE_DIMENSIONS: 'The image exceeds the supported dimensions.',
  IMAGE_TOO_LARGE: 'The image exceeds 10 MiB.',
  UPLOAD_TOO_LARGE: 'The file exceeds 100 MiB.',
  VIDEO_LIMITS: 'The video exceeds the supported format or limits.',
  INVALID_VIDEO: 'The video could not be decoded.',
  INVALID_FRAMES: 'No usable video frames could be extracted.',
  DECODER_UNAVAILABLE: 'Video processing is unavailable on the server.',
  DECODER_TIMEOUT: 'Processing took too long. Try a smaller video.',
  DECODER_INTERRUPTED: 'Processing was interrupted. Please retry.',
  STORAGE_FAILURE: 'The file could not be stored. Please retry.',
};

export function MediaWorkspace({ projectId, finding, findings = [], frameId, onSelectFrame }: { projectId: string; finding?: Finding; findings?: Finding[]; frameId?: string; onSelectFrame?: (id: string) => void }) {
  const cache = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const [selection, setSelection] = useState<string>();
  const shots = useQuery({ queryKey: ['shots', projectId], queryFn: ({ signal }) => listShots(projectId, signal), retry: false });
  const upload = useMutation({
    mutationFn: (selected: File) => uploadShot(projectId, selected),
    onSuccess: shot => { setFile(null); setSelection(shot.frames[0]?.id); },
    onSettled: () => cache.invalidateQueries({ queryKey: ['shots', projectId] }),
  });
  const frames = shots.data?.flatMap(shot => shot.frames.map(frame => ({ ...frame, shot }))) ?? [];
  const selected = frames.find(frame => frame.id === (frameId ?? selection)) ?? frames[0];
  const inspectingOtherFrame = frameId && finding && !finding.relevantFrameIds.includes(frameId);
  const pair = finding ? comparisonPair(finding, shots.data ?? [], frameId) : undefined;
  const comparedIds = [pair?.first?.frame.id, pair?.second?.frame.id];
  function selectFrame(id: string | undefined) { if (id) { setSelection(id); onSelectFrame?.(id); } }
  return <section data-tour="media" className="viewer-panel media-workspace" aria-label="Media workspace">
    <Stack p="md">
      <details className="import-panel" open={!finding}><summary>Import a shot</summary><Stack mt="md">
      <Text size="xs" c="dimmed">JPEG/PNG up to 10 MiB and 16 MP, or MP4/H.264 up to 100 MiB, 120 seconds, 3840 × 2160 and 60 fps. One file per import.</Text>
      <Group align="end"><FileInput className="media-file" label="Shot file" placeholder="Choose an image or video" accept="image/jpeg,image/png,video/mp4" value={file} onChange={setFile} disabled={upload.isPending} clearable /><Button disabled={!file} loading={upload.isPending} onClick={() => file && upload.mutate(file)}>Import shot</Button></Group>
      {upload.isPending && <Text role="status" size="sm">Uploading and processing your shot…</Text>}
      {upload.isError && <Alert color="red" title="Import failed" role="alert">{upload.error.message} Your selected file is available to retry.</Alert>}
      </Stack></details>
      {shots.isPending && <Text role="status">Loading shots…</Text>}
      {shots.isError && <Alert color="red" title="Could not load shots" role="alert"><Button onClick={() => void shots.refetch()}>Retry loading shots</Button></Alert>}
      {finding && !shots.isPending && !shots.isError && !inspectingOtherFrame ? <EvidenceComparison key={finding.id} finding={finding} shots={shots.data ?? []} frameId={frameId} onSelectFrame={selectFrame} /> : selected ? <><Image className="selected-frame" src={selected.url} alt={`Frame from ${selected.shot.name}`} fit="contain" /><Text size="sm">{selected.shot.name} · {selected.timestampMs === null ? 'Still image' : `${(selected.timestampMs / 1000).toFixed(3)} s`}</Text>{inspectingOtherFrame && <Button variant="light" onClick={() => selectFrame(finding.relevantFrameIds[0])}>Back to finding evidence</Button>}</> : !shots.isPending && !shots.isError && <div className="media-empty"><Title order={2} size="h3">Your sequence belongs here.</Title><Text size="sm" c="dimmed">Import your first shot to inspect its frames.</Text></div>}
      <Group justify="space-between"><Text className="panel-label">SHOT TIMELINE</Text><Text size="xs">{shots.data?.filter(shot => shot.status === 'READY').length ?? 0} SHOTS</Text></Group>
      {!!findings.length && <Text size="xs" c="dimmed">Markers show findings on the current page. Affected shots are highlighted for the selected finding.</Text>}
      {finding && <Group gap="xs" aria-label="Affected shots in timeline">{shots.data?.filter(shot => finding.affectedShotIds.includes(shot.id)).map(shot => <Button key={shot.id} size="compact-xs" variant="light" onClick={() => { const target = document.getElementById(`shot-${shot.id}`); target?.scrollIntoView({ block: 'nearest' }); const evidence = shot.frames.find(frame => finding.relevantFrameIds.includes(frame.id)); if (evidence) { selectFrame(evidence.id); document.getElementById(`frame-${evidence.id}`)?.focus({ preventScroll: true }); } }}>Shot {shot.position + 1}</Button>)}</Group>}
      <div className="media-timeline">{shots.data?.map(shot => <div key={shot.id} id={`shot-${shot.id}`} className={`shot-card${finding?.affectedShotIds.includes(shot.id) ? ' shot-affected' : ''}`}><Group justify="space-between"><Text size="xs">{shot.position + 1}. {shot.name}</Text>{findings.some(item => item.affectedShotIds.includes(shot.id)) && <span className="timeline-marker">{finding?.affectedShotIds.includes(shot.id) ? 'Affected · selected finding' : 'Finding'}</span>}</Group>{shot.status === 'FAILED' ? <Text size="xs" c="red.3">Import failed · {failureMessages[shot.failureCode ?? ''] ?? 'Processing failed. Please retry.'}</Text> : <Group gap={6}>{shot.frames.map(frame => <button type="button" key={frame.id} id={`frame-${frame.id}`} className={`frame-button${finding?.relevantFrameIds.includes(frame.id) ? ' frame-evidence' : ''}`} aria-label={`Select ${shot.name} frame ${frame.position + 1}`} aria-pressed={finding && !inspectingOtherFrame ? comparedIds.includes(frame.id) : selected?.id === frame.id} onClick={() => selectFrame(frame.id)}><img src={frame.url} alt="" loading="lazy" /><span>{frame.timestampMs === null ? 'Still' : `${(frame.timestampMs / 1000).toFixed(2)} s`}{finding?.relevantFrameIds.includes(frame.id) ? ' · Evidence' : ''}</span></button>)}</Group>}</div>)}</div>
    </Stack>
  </section>;
}
