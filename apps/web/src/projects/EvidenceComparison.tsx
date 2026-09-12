import { useState } from 'react';
import { Alert, Button, Group, Text, Title } from '@mantine/core';
import type { Finding, Frame, Shot } from '@sceneproof/api-client';

export function mapEvidence(finding: Finding, shots: Shot[]) {
  return shots.filter(shot => finding.affectedShotIds.includes(shot.id))
    .flatMap(shot => shot.frames.filter(frame => finding.relevantFrameIds.includes(frame.id)).map(frame => ({ frame, shot })))
    .sort((first, second) => first.shot.position - second.shot.position || first.frame.position - second.frame.position || first.frame.id.localeCompare(second.frame.id));
}

export function comparisonPair(finding: Finding, shots: Shot[], frameId?: string) {
  const evidence = mapEvidence(finding, shots);
  const first = evidence[0];
  const primarySecond = evidence.find(item => item.shot.id !== first?.shot.id) ?? evidence[1];
  const focused = evidence.find(item => item.frame.id === frameId);
  const second = focused && focused.frame.id !== first?.frame.id ? focused : primarySecond;
  return { evidence, first, second };
}

function EvidenceImage({ frame, shot }: { frame: Frame; shot: Shot }) {
  const [failed, setFailed] = useState(false);
  const [attempt, setAttempt] = useState(0);
  return <figure className="evidence-frame">
    {failed ? <Alert color="red" title="Evidence image unavailable" role="alert"><Button variant="light" onClick={() => { setFailed(false); setAttempt(value => value + 1); }}>Retry image</Button></Alert> : <img key={attempt} src={frame.url} alt={`Evidence: ${shot.name}, frame ${frame.position + 1}`} onError={() => setFailed(true)} />}
    <figcaption>Shot {shot.position + 1} · {shot.name}<span>Frame {frame.position + 1} · {frame.timestampMs === null ? 'Still image' : `${(frame.timestampMs / 1000).toFixed(3)} s`}</span></figcaption>
  </figure>;
}

export function EvidenceComparison({ finding, shots, frameId, onSelectFrame }: { finding: Finding; shots: Shot[]; frameId?: string; onSelectFrame: (id: string) => void }) {
  const { evidence, first, second } = comparisonPair(finding, shots, frameId);
  const missing = finding.relevantFrameIds.filter(id => !evidence.some(item => item.frame.id === id)).length;
  return <section className="evidence-viewer" aria-labelledby="evidence-title">
    <Group justify="space-between"><Title order={2} size="h4" id="evidence-title" tabIndex={-1}>Finding evidence</Title><Text size="xs">{evidence.length} available frames</Text></Group>
    <Text size="sm" c="dimmed" mt="xs">{finding.title}</Text>
    {missing > 0 && <Alert color="yellow" role="alert" mt="sm">{missing} evidence frame(s) unavailable. Only linked, persisted frames are shown.</Alert>}
    {!first && <Text role="status" mt="md">No evidence frames are available for this finding.</Text>}
    {first && <div className={`evidence-pair${second ? '' : ' evidence-single'}`}><EvidenceImage key={first.frame.id} {...first} />{second && <EvidenceImage key={second.frame.id} {...second} />}</div>}
    {first && !second && <Text size="xs" c="dimmed">Only one evidence frame is available; a two-frame comparison is not possible.</Text>}
    {first && evidence.length > 2 && <div><Text size="xs" c="dimmed">First evidence stays on the left. Choose another frame to compare on the right.</Text><Group gap="xs" mt="xs">{evidence.filter(item => item.frame.id !== first.frame.id).map(({ frame, shot }) => <Button key={frame.id} variant="default" size="compact-xs" aria-pressed={second?.frame.id === frame.id} onClick={() => onSelectFrame(frame.id)}>Shot {shot.position + 1} / frame {frame.position + 1}</Button>)}</Group></div>}
  </section>;
}
