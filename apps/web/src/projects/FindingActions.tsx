import { useRef, useState } from 'react';
import { Alert, Badge, Button, Group, Stack, Text, Textarea, Title } from '@mantine/core';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { createFindingAction, listFindingActions, type CreateFindingAction, type Finding } from '@sceneproof/api-client';

const labels = { INTENT_ACCEPTED: 'Intent explains the difference', ISSUE_REMAINS: 'The continuity issue remains', INSUFFICIENT_EVIDENCE: 'Insufficient evidence to decide' };

export function FindingActions({ projectId, finding }: { projectId: string; finding: Finding }) {
  const queries = useQueryClient();
  const storageKey = `sceneproof-action:${projectId}:${finding.id}`;
  const [recovery, setRecovery] = useState<CreateFindingAction | undefined>(() => {
    try {
      const saved = sessionStorage.getItem(storageKey);
      return saved ? JSON.parse(saved) as CreateFindingAction : undefined;
    } catch { return undefined; }
  });
  const [type, setType] = useState<CreateFindingAction['type']>();
  const [explanation, setExplanation] = useState('');
  const [scope, setScope] = useState('');
  const [pending, setPending] = useState(false);
  const submitting = useRef(false);
  const [error, setError] = useState('');
  const [feedback, setFeedback] = useState('');
  const history = useQuery({ queryKey: ['finding-actions', projectId, finding.id], queryFn: ({ signal }) => listFindingActions(projectId, finding.id, signal), retry: false });
  const running = history.data?.some(action => action.reanalysis?.status === 'RUNNING') ?? false;
  const latest = history.data?.filter(action => !action.reanalysis || action.reanalysis.status === 'SUCCEEDED').at(-1);

  async function submit(body: CreateFindingAction) {
    if (submitting.current) return;
    submitting.current = true;
    setPending(true); setError(''); setFeedback('');
    try {
      sessionStorage.setItem(storageKey, JSON.stringify(body));
      setRecovery(body);
      const action = await createFindingAction(projectId, finding.id, body);
      if (action.reanalysis?.status === 'RUNNING') {
        setFeedback('Re-evaluation is still running. Refresh history to read its saved result.');
      } else {
        sessionStorage.removeItem(storageKey); setRecovery(undefined); setType(undefined);
        setFeedback(action.reanalysis?.status === 'FAILED' ? 'The saved attempt failed. No second model call was made.' : 'Creator action saved.');
      }
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Could not retrieve the action result. Recover using the same request.');
    } finally {
      submitting.current = false;
      setPending(false);
      await Promise.all([queries.invalidateQueries({ queryKey: ['findings', projectId] }), queries.invalidateQueries({ queryKey: ['finding-actions', projectId, finding.id] })]);
    }
  }

  function open(nextType: CreateFindingAction['type']) {
    setType(nextType); setExplanation(''); setScope(''); setError(''); setFeedback('');
  }

  return <section aria-labelledby="creator-context-title"><Stack gap="sm">
    <Group justify="space-between"><Title order={3} size="h5" id="creator-context-title">Creator context & history</Title><Badge color={finding.status === 'OPEN' ? 'orange' : 'gray'}>{finding.status}</Badge></Group>
    {latest?.result && <Alert color={latest.result.outcome === 'INTENT_ACCEPTED' ? 'teal' : 'yellow'} title={labels[latest.result.outcome]}>
      <Text size="sm">{latest.result.summary}</Text><Text size="sm" mt="xs">{latest.result.explanation}</Text>
      <Text size="xs" mt="xs">Evaluated scope: {latest.result.evaluatedScope}</Text>
      {latest.result.remainingIssue && <Text size="sm" mt="xs">Remaining concern: {latest.result.remainingIssue}</Text>}
      {latest.result.suggestedCorrection && <Text size="sm" className="correction-prompt" mt="xs">Current suggested correction: {latest.result.suggestedCorrection}</Text>}
    </Alert>}
    {finding.status !== 'OPEN' && <Text size="sm">{finding.status === 'RESOLVED' ? 'Marked corrected by the creator. This is not a new model assessment.' : finding.status === 'DISMISSED' ? 'The creator chose not to treat this finding. This is not acceptance of intent.' : 'Astra judged that the declared intent explains the difference.'} Original finding and evidence remain below.</Text>}
    {(pending || running) && <Text role="status">{running || recovery?.type === 'INTENTIONAL_CHANGE' ? 'Re-evaluating the original evidence with creator context… No result is assumed.' : 'Saving the creator action…'}</Text>}
    {feedback && <Text role="status" size="sm">{feedback}</Text>}
    {error && <Alert color="red" title="Action result unavailable" role="alert">{error} The previous inspection is preserved.</Alert>}
    {recovery && !pending && <Button variant="light" onClick={() => void submit(recovery)}>Recover same request · replay-safe</Button>}
    {finding.status === 'OPEN' && !type && !recovery && <Group gap="xs">
      <Button variant="light" disabled={running || history.isPending || history.isError} onClick={() => open('INTENTIONAL_CHANGE')}>This change is intentional</Button>
      <Button variant="subtle" disabled={running || history.isPending || history.isError} onClick={() => open('RESOLVE')}>Resolve</Button>
      <Button variant="subtle" disabled={running || history.isPending || history.isError} onClick={() => open('DISMISS')}>Dismiss</Button>
    </Group>}
    {type && !recovery && <form onSubmit={event => {
      event.preventDefault();
      if (!explanation.trim() || (type === 'INTENTIONAL_CHANGE' && !scope.trim())) { setError('Provide an explanation and the narrative scope.'); return; }
      void submit({ requestId: crypto.randomUUID(), type, explanation: explanation.trim(), scope: type === 'INTENTIONAL_CHANGE' ? scope.trim() : 'This finding and its affected shots.', affectedShotIds: finding.affectedShotIds });
    }}><Stack gap="sm">
      <Text size="sm">{type === 'INTENTIONAL_CHANGE' ? 'Your explanation adds narrative context. Astra may agree, maintain the issue, or find insufficient evidence. Confirming starts one targeted paid re-evaluation.' : type === 'RESOLVE' ? 'Record that you corrected this issue. No Astra call.' : 'Record why you choose not to treat this finding. No Astra call.'}</Text>
      <Textarea label={type === 'INTENTIONAL_CHANGE' ? 'Why is this change intentional?' : 'Creator note'} required maxLength={2000} value={explanation} onChange={event => setExplanation(event.currentTarget.value)} autosize minRows={3} />
      {type === 'INTENTIONAL_CHANGE' && <Textarea label="Narrative scope" description="Describe where this explanation applies across the affected shots shown below." required maxLength={1000} value={scope} onChange={event => setScope(event.currentTarget.value)} autosize minRows={2} />}
      <Group><Button type="submit">{type === 'INTENTIONAL_CHANGE' ? 'Confirm intent & re-evaluate' : type === 'RESOLVE' ? 'Confirm resolve' : 'Confirm dismiss'}</Button><Button variant="subtle" onClick={() => setType(undefined)}>Cancel</Button></Group>
    </Stack></form>}
    <details><summary>View immutable history ({history.data?.length ?? '…'} creator actions)</summary><Stack gap="sm" mt="sm">
      <Text size="sm">Original analysis: {finding.analysisRunId}. Original finding, reasoning and evidence are preserved below.</Text>
      {history.isPending && <Text role="status">Loading history…</Text>}
      {history.isError && <Alert color="red" role="alert">Could not load history. {history.error.message}</Alert>}
      {history.data?.map(action => <article className="creator-history-entry" key={action.id}>
        <Text fw={600} size="sm">{action.type.replaceAll('_', ' ')} · {action.reanalysis?.status ?? 'SAVED'}</Text>
        <Text size="xs" c="dimmed">{new Date(action.createdAt).toLocaleString()} · {action.id}</Text>
        <Text size="sm">Creator: {action.explanation}</Text><Text size="xs">Declared scope: {action.scope}</Text>
        {action.supersedesActionId && <Text size="xs">{action.reanalysis && action.reanalysis.status !== 'SUCCEEDED' ? 'Would supersede' : 'Supersedes judgement'}: {action.supersedesActionId}</Text>}
        {action.result && <><Text size="sm" fw={600}>{labels[action.result.outcome]}</Text><Text size="sm">{action.result.summary}</Text><Text size="sm">{action.result.explanation}</Text><Text size="xs">Evaluated scope: {action.result.evaluatedScope}</Text>{action.result.remainingIssue && <Text size="sm">Remaining concern: {action.result.remainingIssue}</Text>}{action.result.suggestedCorrection && <Text size="sm" className="correction-prompt">Suggested correction at this judgement: {action.result.suggestedCorrection}</Text>}</>}
        {action.reanalysis?.status === 'FAILED' && <Text size="sm" c="red.3" role="alert">{action.reanalysis.failureMessage} This attempt changed no judgement. A new confirmed intent action is a separate paid attempt.</Text>}
      </article>)}
    </Stack></details>
    <Button variant="subtle" size="compact-xs" loading={history.isFetching} onClick={() => { void history.refetch(); void queries.invalidateQueries({ queryKey: ['findings', projectId] }); }}>Refresh history</Button>
    {latest && <Text size="xs" c="dimmed">The original model assessment follows. Use the current judgement above when interpreting it.</Text>}
  </Stack></section>;
}
