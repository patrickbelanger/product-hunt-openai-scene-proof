import { useState } from 'react';
import { Alert, Button, FileInput, Group, Image, Stack, Text, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { listShots, uploadShot } from '@sceneproof/api-client';

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

export function MediaWorkspace({ projectId }: { projectId: string }) {
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
  const selected = frames.find(frame => frame.id === selection) ?? frames[0];
  return <section className="viewer-panel media-workspace" aria-label="Media workspace">
    <Stack p="md">
      <Title order={2} size="h4">Import a shot</Title>
      <Text size="xs" c="dimmed">JPEG/PNG up to 10 MiB and 16 MP, or MP4/H.264 up to 100 MiB, 120 seconds, 3840 × 2160 and 60 fps. One file per import.</Text>
      <Group align="end"><FileInput className="media-file" label="Shot file" placeholder="Choose an image or video" accept="image/jpeg,image/png,video/mp4" value={file} onChange={setFile} disabled={upload.isPending} clearable /><Button disabled={!file} loading={upload.isPending} onClick={() => file && upload.mutate(file)}>Import shot</Button></Group>
      {upload.isPending && <Text role="status" size="sm">Uploading and processing your shot…</Text>}
      {upload.isError && <Alert color="red" title="Import failed" role="alert">{upload.error.message} Your selected file is available to retry.</Alert>}
      {shots.isPending && <Text role="status">Loading shots…</Text>}
      {shots.isError && <Alert color="red" title="Could not load shots" role="alert"><Button onClick={() => void shots.refetch()}>Retry loading shots</Button></Alert>}
      {selected ? <><Image className="selected-frame" src={selected.url} alt={`Frame from ${selected.shot.name}`} fit="contain" /><Text size="sm">{selected.shot.name} · {selected.timestampMs === null ? 'Still image' : `${(selected.timestampMs / 1000).toFixed(3)} s`}</Text></> : <div className="media-empty"><Title order={2} size="h3">Your sequence belongs here.</Title><Text size="sm" c="dimmed">Import your first shot to inspect its frames. Continuity analysis is coming next.</Text></div>}
      <Group justify="space-between"><Text className="panel-label">SHOT TIMELINE</Text><Text size="xs">{shots.data?.filter(shot => shot.status === 'READY').length ?? 0} SHOTS</Text></Group>
      <div className="media-timeline">{shots.data?.map(shot => <div key={shot.id} className="shot-card"><Text size="xs">{shot.position + 1}. {shot.name}</Text>{shot.status === 'FAILED' ? <Text size="xs" c="red.3">Import failed · {failureMessages[shot.failureCode ?? ''] ?? 'Processing failed. Please retry.'}</Text> : <Group gap={6}>{shot.frames.map(frame => <button type="button" key={frame.id} className="frame-button" aria-label={`Select ${shot.name} frame ${frame.position + 1}`} aria-pressed={selected?.id === frame.id} onClick={() => setSelection(frame.id)}><img src={frame.url} alt="" loading="lazy" /><span>{frame.timestampMs === null ? 'Still' : `${(frame.timestampMs / 1000).toFixed(2)} s`}</span></button>)}</Group>}</div>)}</div>
    </Stack>
  </section>;
}
