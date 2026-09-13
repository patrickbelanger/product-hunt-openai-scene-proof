import { useState } from 'react';
import { Alert, Badge, Button, Group, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { getAnalysis, listFindings, listShots, type Finding } from '@sceneproof/api-client';
import { MediaWorkspace } from './MediaWorkspace';
import { FindingActions } from './FindingActions';

export function findingLabel(value: string) {
  return value.toLowerCase().replaceAll('_', ' ').replace(/^./, letter => letter.toUpperCase());
}

function Correction({ prompt }: { prompt: string }) {
  const [state, setState] = useState<'idle' | 'pending' | 'success' | 'error'>('idle');
  async function copy() {
    setState('pending');
    try {
      await navigator.clipboard.writeText(prompt);
      setState('success');
    } catch {
      setState('error');
    }
  }
  return <section aria-labelledby="correction-title">
    <Title order={3} size="h5" id="correction-title">Suggested correction prompt</Title>
    <Text size="sm" className="correction-prompt" mt="sm">{prompt}</Text>
    <Button variant="light" mt="sm" loading={state === 'pending'} onClick={() => void copy()}>Copy correction</Button>
    {state === 'success' && <Text size="sm" c="teal.3" role="status" mt="xs">Correction copied.</Text>}
    {state === 'error' && <Text size="sm" c="red.3" role="alert" mt="xs">Could not copy. Select the prompt text and copy it manually, or try again.</Text>}
  </section>;
}

export function FindingsWorkspace({ projectId }: { projectId: string }) {
  const [params, setParams] = useSearchParams();
  const analysisId = params.get('analysisId') || undefined;
  const selectedId = params.get('finding');
  const pageValue = Number(params.get('findingsPage') ?? 0);
  const page = Number.isInteger(pageValue) && pageValue >= 0 && pageValue <= 10000 ? pageValue : 0;
  const [navigation, setNavigation] = useState<{ findingId: string | null; frameId: string }>();
  const frameId = navigation?.findingId === selectedId ? navigation?.frameId : undefined;
  function setFrameId(id?: string) { setNavigation(id ? { findingId: selectedId, frameId: id } : undefined); }
  const shots = useQuery({ queryKey: ['shots', projectId], queryFn: ({ signal }) => listShots(projectId, signal), retry: false });
  const findings = useQuery({ queryKey: ['findings', projectId, analysisId, page], queryFn: ({ signal }) => listFindings(projectId, analysisId, page, signal), retry: false });
  const analysis = useQuery({ queryKey: ['analysis', projectId, analysisId], queryFn: ({ signal }) => getAnalysis(projectId, analysisId!, signal), enabled: !!analysisId, retry: false });
  const selected = findings.data?.find(finding => finding.id === selectedId);
  function select(finding: Finding) {
    setFrameId(undefined);
    setParams(previous => { const next = new URLSearchParams(previous); next.set('finding', finding.id); next.set('analysisId', finding.analysisRunId); next.delete('findingsPage'); return next; });
  }
  function changePage(nextPage: number) {
    setFrameId(undefined);
    setParams(previous => { const next = new URLSearchParams(previous); next.set('findingsPage', String(nextPage)); next.delete('finding'); return next; });
  }
  function navigateShot(shotId: string) {
    const shot = shots.data?.find(item => item.id === shotId);
    const frame = shot?.frames.find(item => selected?.relevantFrameIds.includes(item.id));
    if (frame) setFrameId(frame.id);
    const target = document.getElementById(`shot-${shotId}`);
    target?.scrollIntoView({ block: 'nearest', inline: 'nearest' });
    if (frame) document.getElementById(`frame-${frame.id}`)?.focus({ preventScroll: true });
  }
  return <>
    <MediaWorkspace projectId={projectId} finding={selected} findings={(findings.data ?? []).filter(finding => finding.status === 'OPEN')} frameId={frameId} onSelectFrame={setFrameId} />
    <aside className="workspace-panel findings-panel" aria-labelledby="findings-title">
      <Stack gap="md">
        <Group justify="space-between"><Text className="panel-label" id="findings-title">CONTINUITY FINDINGS</Text><Button size="compact-xs" variant="subtle" loading={findings.isFetching || analysis.isFetching} onClick={() => { void findings.refetch(); if (analysisId) void analysis.refetch(); }}>Refresh findings</Button></Group>
        <Text size="xs" c="dimmed">{analysisId ? 'Results for the linked analysis.' : 'Saved findings, newest analysis first. Earlier analyses remain in history.'} Refresh only reads saved results.</Text>
        {analysisId && <Button variant="subtle" size="compact-xs" onClick={() => { setFrameId(undefined); setParams(previous => { const next = new URLSearchParams(previous); next.delete('analysisId'); next.delete('finding'); next.delete('findingsPage'); return next; }); }}>All saved findings</Button>}
        {analysisId && analysis.isPending && <Text role="status">Loading analysis status…</Text>}
        {analysis.isError && <Alert color="red" title="Analysis status unavailable" role="alert">{analysis.error.message}</Alert>}
        {analysis.data?.status === 'FAILED' && <Alert color="red" title="Analysis failed" role="alert">{analysis.data.failureMessage ?? 'This analysis failed. No findings were saved.'}</Alert>}
        {analysis.data?.status === 'RUNNING' && <Text role="status">Analysis is running. Refresh to check its saved results.</Text>}
        {findings.isPending && <Text role="status">Loading findings…</Text>}
        {findings.isError && <Alert color="red" title="Findings unavailable" role="alert"><Text size="sm">{findings.error.message}</Text><Button mt="sm" variant="light" onClick={() => void findings.refetch()}>Retry loading findings</Button></Alert>}
        {findings.isSuccess && findings.data.length === 0 && <div className="findings-empty"><Title order={2} size="h4">{analysis.data?.kind === 'TARGETED' ? 'Targeted re-evaluation' : analysis.data?.status === 'SUCCEEDED' ? 'No findings in this analysis.' : 'No saved findings here.'}</Title><Text size="sm" c="dimmed" mt="sm">{analysis.data?.kind === 'TARGETED' ? 'This run evaluates one existing finding. Open All saved findings and its creator history to inspect the judgement.' : analysis.data?.status === 'SUCCEEDED' ? 'This saved analysis reported no continuity issues.' : 'An empty list does not confirm that the project has been reviewed. Analysis is started separately through the API.'}</Text></div>}
        {selectedId && findings.isSuccess && !selected && <Alert color="yellow" title="Finding not available" role="alert">The linked finding is not on this page or in this analysis. Choose a saved finding below.</Alert>}
        {!!findings.data?.length && <><Text size="xs" c="dimmed">Page {page + 1} · {findings.data.length} saved findings on this page · {findings.data.filter(finding => finding.status === 'OPEN').length} open</Text><ul className="finding-list">{findings.data.map(finding => <li key={finding.id}><button type="button" className="finding-button" aria-pressed={selected?.id === finding.id} onClick={() => select(finding)}><span className={`severity-label severity-${finding.severity.toLowerCase()}`}>{findingLabel(finding.severity)} severity · {findingLabel(finding.category)} · {finding.status}</span><strong>{finding.title}</strong><span>{finding.summary}</span></button></li>)}</ul></>}
        {(page > 0 || findings.data?.length === 20) && <Group><Button variant="default" disabled={page === 0 || findings.isFetching} onClick={() => changePage(page - 1)}>Previous findings</Button><Button variant="default" disabled={findings.data?.length !== 20 || findings.isFetching || page === 10000} onClick={() => changePage(page + 1)}>Next findings</Button></Group>}
        {selected && <section className="finding-detail" aria-labelledby="finding-detail-title"><Stack gap="lg">
          <FindingActions key={selected.id} projectId={projectId} finding={selected} />
          <div><Text size="xs" c="dimmed" role="status">Selected finding · evidence shown in viewer</Text><Title order={2} size="h4" id="finding-detail-title" mt="xs">{selected.title}</Title><Group gap="xs" mt="sm"><Badge color={selected.severity === 'HIGH' ? 'orange' : selected.severity === 'MEDIUM' ? 'yellow' : 'gray'}>{findingLabel(selected.severity)} severity</Badge><Text size="xs">{findingLabel(selected.category)}</Text></Group><Text size="sm" mt="xs">Model confidence: {Math.round(selected.confidence * 100)}%</Text><Text size="xs" c="dimmed">The model’s reported confidence, not a guarantee.</Text><Text size="sm" mt="sm">{selected.summary}</Text></div>
          <div><Title order={3} size="h5">Affected shots</Title><Group gap="xs" mt="sm">{selected.affectedShotIds.map(shotId => { const shot = shots.data?.find(item => item.id === shotId); return <Button key={shotId} className="affected-shot-link" size="compact-sm" variant="light" disabled={!shot} onClick={() => navigateShot(shotId)}>{shot ? `Shot ${shot.position + 1} · ${shot.name}` : 'Shot unavailable'}</Button>; })}</Group><Button variant="subtle" size="compact-sm" mt="sm" onClick={() => { const viewer = document.getElementById('evidence-title'); viewer?.scrollIntoView({ block: 'start' }); viewer?.focus({ preventScroll: true }); }}>View evidence comparison</Button></div>
          <dl className="finding-states"><dt>Expected state</dt><dd>{selected.expectedState}</dd><dt>Observed state</dt><dd>{selected.observedState}</dd></dl>
          <div><Title order={3} size="h5">Why it matters</Title><Text size="sm" mt="sm" className="rules-text">{selected.explanation}</Text></div>
          <Correction key={selected.id} prompt={selected.suggestedCorrectionPrompt} />
        </Stack></section>}
      </Stack>
    </aside>
  </>;
}
