import { Alert, Badge, Button, FileInput, Group, Modal, Paper, Select, Stack, Text, Textarea, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FilmProgress, isActive, stageLabels } from './FilmProgress';
import { SourceProvenance } from './SourceProvenance';
import { FilmLimitations, UnderstandingSummary } from './UnderstandingSummary';
import { decideFilmCandidate, getFilmCandidates, getFilmIntelligence, getFilmTranscript, promoteFilmCandidate, understandFilm, uploadSourceFilm, type DecideFilmCandidate, type FilmCandidate, type FilmEvidence, type FilmIntelligence as Intelligence, type TranscriptSegment, type UnderstandFilmRequest } from '@sceneproof/api-client';

const timestamp = (milliseconds: number) => `${(milliseconds / 1000).toFixed(2)}s`;

function FilmEvidenceView({ evidence, intelligence, transcript }: { evidence: FilmEvidence; intelligence: Intelligence; transcript: TranscriptSegment[] }) {
  const [expandedFrame, setExpandedFrame] = useState<string | null>(null);
  const frames = intelligence.segments.flatMap(segment => segment.shot.frames.map(frame => ({ ...frame, sourceTimestampMs: segment.startMs + (frame.timestampMs ?? 0) })));
  const inspected = frames.find(frame => frame.id === expandedFrame);
  return <details className="film-evidence"><summary>Inspect supporting evidence ({evidence.frameIds.length} frames, {evidence.transcriptSegmentIds.length} transcript segments)</summary>
    <div className="film-frame-grid">{evidence.frameIds.map(id => {
      const frame = frames.find(value => value.id === id);
      return frame ? <figure key={id}><button className="film-thumbnail" type="button" onClick={() => setExpandedFrame(id)} aria-label={`Enlarge source frame at ${timestamp(frame.sourceTimestampMs)}`}><img src={frame.url} alt={`Source film at ${timestamp(frame.sourceTimestampMs)}`} loading="lazy" onError={event => { event.currentTarget.alt = 'Source evidence image unavailable'; }} /></button><figcaption>{timestamp(frame.sourceTimestampMs)} · decoded frame</figcaption></figure> : <Text key={id} c="red.3">Frame metadata unavailable.</Text>;
    })}</div>
    {evidence.transcriptSegmentIds.map(id => { const segment = transcript.find(value => value.id === id); return segment ? <blockquote key={id}><Text size="xs" c="dimmed">{timestamp(segment.startMs)}–{timestamp(segment.endMs)} · approximate transcription timestamps</Text><Text size="sm">{segment.text}</Text></blockquote> : <Text key={id} c="dimmed">Transcript evidence unavailable; retry loading the transcript.</Text>; })}
    <Modal opened={!!inspected} onClose={() => setExpandedFrame(null)} title={inspected ? `Visual evidence · source ${timestamp(inspected.sourceTimestampMs)}` : 'Visual evidence'} size="xl">
      {inspected && <><img className="film-expanded-evidence" src={inspected.url} alt={`Full source evidence at ${timestamp(inspected.sourceTimestampMs)}`} /><Text size="xs">Decoded sampled frame · source time {timestamp(inspected.sourceTimestampMs)}. Intervening action may not be observed.</Text></>}
    </Modal>
  </details>;
}

function CandidateReview({ candidate, projectId, intelligence, transcript }: { candidate: FilmCandidate; projectId: string; intelligence: Intelligence; transcript: TranscriptSegment[] }) {
  const client = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState(candidate.proposal.title);
  const [rule, setRule] = useState(candidate.proposal.rule);
  const [scope, setScope] = useState(candidate.proposal.scope);
  const refresh = async () => { await client.invalidateQueries({ queryKey: ['film', projectId] }); await client.invalidateQueries({ queryKey: ['film-candidates', projectId] }); await client.invalidateQueries({ queryKey: ['references', projectId] }); };
  const decision = useMutation({ mutationFn: (body: DecideFilmCandidate) => decideFilmCandidate(projectId, candidate.id, body), onSuccess: async () => { setEditing(false); await refresh(); } });
  const promotion = useMutation({ mutationFn: () => promoteFilmCandidate(projectId, candidate.id), onSuccess: refresh });
  const pending = decision.isPending || promotion.isPending;
  const confirmed = candidate.status === 'ACCEPTED' || candidate.status === 'EDITED';
  return <Paper withBorder p="md" className="film-candidate">
    <Group justify="space-between"><Title order={4} size="h5">{candidate.confirmedTitle ?? candidate.proposal.title}</Title><Badge variant="light">{candidate.status === 'PENDING' ? 'Proposed · not a rule' : candidate.status}</Badge></Group>
    <Text size="sm" mt="xs">{candidate.confirmedRule ?? candidate.proposal.rule}</Text><Text size="xs" mt="xs">Scope: {candidate.confirmedScope ?? candidate.proposal.scope}</Text>
    {candidate.proposal.uncertainty && <Text size="xs" c="dimmed" mt="xs">Uncertainty: {candidate.proposal.uncertainty}</Text>}
    <FilmEvidenceView evidence={candidate.proposal.evidence} intelligence={intelligence} transcript={transcript} />
    {candidate.status === 'PENDING' && <Group mt="sm"><Button size="compact-sm" disabled={pending} onClick={() => decision.mutate({ status: 'ACCEPTED', title: '', rule: '', scope: '' })}>Accept anchor</Button><Button size="compact-sm" variant="default" disabled={pending} onClick={() => setEditing(true)}>Edit anchor</Button><Button size="compact-sm" variant="subtle" disabled={pending} onClick={() => decision.mutate({ status: 'REJECTED', title: '', rule: '', scope: '' })}>Reject anchor</Button></Group>}
    {confirmed && <Text size="xs" c="teal.3" mt="sm">Creator-confirmed memory for later continuity review.</Text>}
    {confirmed && candidate.proposal.evidence.frameIds.length > 0 && <Button size="compact-sm" variant="light" mt="sm" disabled={pending || !!candidate.referenceId} onClick={() => promotion.mutate()}>{candidate.referenceId ? 'Added to Reference Bible' : 'Add visual reference to Bible'}</Button>}
    {(decision.error || promotion.error) && <Alert role="alert" color="red" mt="sm">{(decision.error ?? promotion.error)?.message}</Alert>}
    <Modal opened={editing} onClose={() => { if (!pending) setEditing(false); }} closeOnEscape={!pending} closeOnClickOutside={!pending} withCloseButton={!pending} title="Edit and confirm anchor">
      <Stack><Text size="sm">Your confirmed wording becomes continuity context. This makes no AI call.</Text><TextInput label="Anchor title" value={title} onChange={event => setTitle(event.currentTarget.value)} maxLength={120} /><Textarea label="Continuity expectation" value={rule} onChange={event => setRule(event.currentTarget.value)} maxLength={2000} autosize minRows={3} /><Textarea label="Narrative scope" value={scope} onChange={event => setScope(event.currentTarget.value)} maxLength={1000} autosize minRows={2} /><Button loading={decision.isPending} disabled={!title.trim() || !rule.trim() || !scope.trim()} onClick={() => decision.mutate({ status: 'EDITED', title, rule, scope })}>Confirm edited anchor</Button>{decision.error && <Alert role="alert" color="red">{decision.error.message}</Alert>}</Stack>
    </Modal>
  </Paper>;
}

export function FilmIntelligence({ projectId }: { projectId: string }) {
  const client = useQueryClient();
  const [params, setParams] = useSearchParams();
  const selectedId = params.get('filmRun');
  const [file, setFile] = useState<File | null>(null);
  const [consent, setConsent] = useState(false);
  const [storageError, setStorageError] = useState('');
  const [manualRefreshing, setManualRefreshing] = useState(false);
  const key = `sceneproof.film-request.v1.${projectId}`;
  const [unresolved, setUnresolved] = useState<UnderstandFilmRequest | null>(() => {
    try { const saved = sessionStorage.getItem(key); return saved ? JSON.parse(saved) as UnderstandFilmRequest : null; } catch { return null; }
  });
  const intelligence = useQuery({ queryKey: ['film', projectId], queryFn: ({ signal }) => getFilmIntelligence(projectId, signal), retry: false, refetchInterval: query => query.state.data?.runs.some(isActive) || unresolved ? 1500 : false });
  const latest = intelligence.data?.runs[0];
  const run = intelligence.data?.runs.find(value => value.id === selectedId) ?? latest;
  const candidates = useQuery({ queryKey: ['film-candidates', projectId, run?.id], queryFn: ({ signal }) => getFilmCandidates(projectId, run!.id, signal), enabled: run?.stage === 'SUCCEEDED', retry: false });
  const transcript = useQuery({ queryKey: ['film-transcript', projectId, run?.id], queryFn: ({ signal }) => getFilmTranscript(projectId, run!.id, signal), enabled: !!run?.audioStatus, retry: false });
  const upload = useMutation({ mutationFn: () => uploadSourceFilm(projectId, file!), onSuccess: async () => { setFile(null); await intelligence.refetch(); } });
  const understand = useMutation({ mutationFn: (body: UnderstandFilmRequest) => understandFilm(projectId, body), onError: () => setConsent(false), onSuccess: async result => { setConsent(false); setParams(previous => { const next = new URLSearchParams(previous); next.set('filmRun', result.id); return next; }); await intelligence.refetch(); } });
  useEffect(() => {
    if (unresolved && intelligence.data?.runs.some(value => value.requestId === unresolved.requestId)) {
      try { sessionStorage.removeItem(key); } catch { }
      setUnresolved(null);
    }
  }, [intelligence.data, key, unresolved]);
  useEffect(() => { if (latest?.stage === 'SUCCEEDED') void client.invalidateQueries({ queryKey: ['shots', projectId] }); }, [latest?.id, latest?.stage, client, projectId]);
  function start() {
    if (!intelligence.data?.source || unresolved || understand.isPending || isActive(latest)) return;
    const request: UnderstandFilmRequest = { requestId: crypto.randomUUID(), sourceFilmId: intelligence.data.source.id, paidConsent: true };
    try { sessionStorage.setItem(key, JSON.stringify(request)); setStorageError(''); } catch { setStorageError('Browser storage is unavailable. No paid request was sent.'); return; }
    setUnresolved(request);
    understand.mutate(request);
  }
  return <section aria-labelledby="film-intelligence-title" className="film-intelligence">
    <Group justify="space-between"><div><Title id="film-intelligence-title" order={2} size="h4">Film Intelligence</Title><Text size="sm" c="dimmed">Upload the film. Discover what matters. Confirm what should stay consistent.</Text></div><Button variant="subtle" size="compact-sm" loading={manualRefreshing} onClick={async () => { setManualRefreshing(true); try { await intelligence.refetch(); } finally { setManualRefreshing(false); } }}>Refresh now</Button></Group>
    {intelligence.isPending && <Text size="sm" role="status">Loading saved film state…</Text>}
    {intelligence.isError && <Alert color="red" role="alert" mt="sm" title="Film state unavailable">{intelligence.error.message} The last known state may be outdated. Refresh to reconnect.</Alert>}
    {intelligence.data && <Stack mt="md" gap="md">
      {!intelligence.data.source ? <div><Text size="sm">Primary source: MP4/H.264, up to 120 seconds and 100 MiB. Uploading makes no AI call.</Text><Group align="end" mt="xs"><FileInput label="Primary source film" accept="video/mp4" value={file} onChange={setFile} disabled={upload.isPending} /><Button disabled={!file || upload.isPending} loading={upload.isPending} onClick={() => upload.mutate()}>Upload source film</Button></Group>{upload.error && <Alert color="red" role="alert" mt="xs">{upload.error.message}</Alert>}</div> : <Group justify="space-between"><SourceProvenance source={intelligence.data.source} /><Button disabled={isActive(latest) || !!unresolved || understand.isPending || intelligence.isError} onClick={() => { understand.reset(); setConsent(true); }}>{latest ? 'New understanding attempt' : 'Understand film'}</Button></Group>}
      {unresolved && <Alert color="yellow" title="Recover the submitted request"><Text size="sm">A submitted request is not yet confirmed. Recovery reuses its original identity and cannot start a second paid attempt.</Text><Button mt="xs" variant="light" loading={understand.isPending} onClick={() => understand.mutate(unresolved)}>Recover same film request</Button></Alert>}
      {understand.error && <Alert role="alert" color="red">{understand.error.message}</Alert>}
      {storageError && <Alert role="alert" color="red">{storageError}</Alert>}
      {intelligence.data.source && <Text size="xs" c="dimmed">{intelligence.data.confirmedAnchors.length} confirmed anchors</Text>}
      {!!intelligence.data.runs.length && <details className="film-history"><summary>Understanding history ({intelligence.data.runs.length})</summary><Select label="Understanding history" value={run?.id ?? null} data={intelligence.data.runs.map(value => ({ value: value.id, label: `${new Date(value.startedAt).toLocaleString()} · ${stageLabels[value.stage]}` }))} onChange={value => { if (value) setParams(previous => { const next = new URLSearchParams(previous); next.set('filmRun', value); return next; }); }} /></details>}
      {run && <div><FilmProgress run={run} fetching={intelligence.isFetching} updatedAt={intelligence.dataUpdatedAt} disconnected={intelligence.isError} />{run.stage === 'FAILED' && <Alert role="alert" color="red" mt="sm">{run.failureMessage} This failed attempt remains in history. A new attempt needs fresh confirmation.</Alert>}</div>}
      {run?.result && <><UnderstandingSummary result={run.result} />
        <section aria-label="Potential continuity concerns"><Title order={3} size="h5">Potential continuity concerns ({run.result.potentialConcerns.length})</Title><Text size="xs" c="dimmed">Discovery questions, not confirmed errors. Difference alone is not a continuity error.</Text>{!run.result.potentialConcerns.length && <Text size="sm" mt="sm">No supported concerns were proposed. This does not prove the film error-free.</Text>}{run.result.potentialConcerns.map((concern, index) => <Paper className="film-concern" withBorder p="sm" mt="sm" key={index}><Title order={4} size="h5">{concern.title}</Title><Text size="sm" className="concern-preview" mt="xs">{concern.explanation}</Text><Group gap="xs" mt="sm"><Badge color="yellow" variant="light">Needs confirmation</Badge>{concern.evidence.frameIds.length > 0 && <Badge variant="light">Visual evidence · {concern.evidence.frameIds.length} frames</Badge>}{concern.evidence.transcriptSegmentIds.length > 0 && <Badge variant="light">Narrative evidence · {concern.evidence.transcriptSegmentIds.length} transcript segments</Badge>}</Group><details><summary>Concern rationale & uncertainty</summary><Text size="sm">{concern.explanation}</Text><Text size="sm" c="dimmed" mt="xs">{concern.uncertainty}</Text></details><FilmEvidenceView evidence={concern.evidence} intelligence={intelligence.data!} transcript={transcript.data ?? []} /></Paper>)}</section>
        <div><Title order={3} size="h5">Candidate continuity anchors</Title><Text size="xs" c="dimmed">Accept, edit or reject. Only confirmed anchors become continuity expectations. Reviewing candidates makes no AI call.</Text>{candidates.isPending && <Text role="status">Loading candidates…</Text>}{candidates.error && <Alert role="alert" color="red">{candidates.error.message}<Button variant="subtle" onClick={() => void candidates.refetch()}>Retry candidates</Button></Alert>}{candidates.data?.length === 0 && <Text size="sm">No supported anchors were proposed.</Text>}<Stack mt="sm" className="film-candidates-grid">{candidates.data?.map(candidate => <CandidateReview key={candidate.id} candidate={candidate} projectId={projectId} intelligence={intelligence.data!} transcript={transcript.data ?? []} />)}</Stack></div>
        <details><summary>Recurring entities and narrative context</summary>{run.result.entities.map(entity => <Paper withBorder p="sm" mt="sm" key={entity.localId}><Text fw={600}>{entity.name}</Text><Text size="sm">{entity.description}</Text><FilmEvidenceView evidence={entity.evidence} intelligence={intelligence.data!} transcript={transcript.data ?? []} /></Paper>)}{run.result.narrativeCues.map((cue, index) => <Paper withBorder p="sm" mt="sm" key={index}><Text fw={600}>{cue.title}</Text><Text size="sm">{cue.interpretation}</Text><Text size="xs" c="dimmed">{cue.uncertainty}</Text><FilmEvidenceView evidence={cue.evidence} intelligence={intelligence.data!} transcript={transcript.data ?? []} /></Paper>)}</details>
        <FilmLimitations warnings={run.result.warnings} />
      </>}
      {run?.audioStatus && <details><summary>Transcript and audio context</summary><Text size="xs" c="dimmed">{run.audioStatus === 'NO_AUDIO_STREAM' ? 'The source contains no audio stream; understanding uses visual evidence only.' : 'Transcribed words may be lyrics, narration or dialogue. They are fallible context, not proof of on-screen action. Timestamps are approximate.'}</Text>{transcript.error && <Alert role="alert" color="red">{transcript.error.message}<Button variant="subtle" onClick={() => void transcript.refetch()}>Retry transcript</Button></Alert>}{transcript.data?.map(segment => <p key={segment.id}><Text component="span" size="xs" c="dimmed">{timestamp(segment.startMs)}–{timestamp(segment.endMs)} </Text>{segment.text}</p>)}</details>}
    </Stack>}
    <Modal opened={consent} onClose={() => { if (!understand.isPending) setConsent(false); }} closeOnEscape={!understand.isPending} closeOnClickOutside={!understand.isPending} withCloseButton={!understand.isPending} title="Understand this film?">
      <Stack><Text size="sm">This paid action sends sampled film images and extracted audio to OpenAI: at most one transcription and one Astra Film Understanding request. It may incur cost even if interrupted.</Text><Text size="sm">The source remains bounded to 120 seconds, 24 selected frames and one attempt. Results propose candidates for your review; they do not automatically become rules. There are no automatic AI retries.</Text><Group justify="end"><Button data-autofocus variant="default" disabled={understand.isPending} onClick={() => setConsent(false)}>Cancel</Button><Button loading={understand.isPending} onClick={start}>Confirm & understand</Button></Group>{storageError && <Alert role="alert" color="red">{storageError}</Alert>}{understand.error && <Alert role="alert" color="red">{understand.error.message}</Alert>}</Stack>
    </Modal>
  </section>;
}
