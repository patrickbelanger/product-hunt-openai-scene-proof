import { Alert, Button, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { listFindingReferences, type Finding } from '@sceneproof/api-client';
import { ReferenceImage } from './ReferenceImage';

export function FindingReferences({ projectId, finding }: { projectId: string; finding: Finding }) {
  const references = useQuery({ queryKey: ['finding-references', projectId, finding.id], queryFn: ({ signal }) => listFindingReferences(projectId, finding.id, signal), enabled: finding.relevantReferenceIds.length > 0, retry: false });
  if (finding.relevantReferenceIds.length === 0) return null;
  const incomplete = references.isSuccess && (references.data.length !== finding.relevantReferenceIds.length || finding.relevantReferenceIds.some(id => !references.data.some(reference => reference.id === id)));
  return <section aria-labelledby="reference-evidence-title"><Stack gap="sm">
    <Title order={3} size="h5" id="reference-evidence-title">Reference evidence</Title>
    <Text size="xs" c="dimmed">Declared visual truth cited by this finding. Image and guidance as submitted to the original analysis.</Text>
    {references.isPending && <Text role="status" size="sm">Loading cited references…</Text>}
    {(references.isError || incomplete) && <Alert color="red" title="Historical references unavailable" role="alert">The original reference evidence could not be fully loaded.<Button mt="sm" variant="light" size="xs" onClick={() => void references.refetch()}>Retry reference evidence</Button></Alert>}
    {references.data?.map(reference => <figure key={reference.id} className="reference-evidence"><ReferenceImage reference={reference} /><figcaption><strong>{reference.title}</strong><Text size="sm" className="rules-text">{reference.guidance || 'No creator guidance was supplied.'}</Text>{reference.archivedAt && <Text size="xs" c="dimmed">Archived from the current Bible · retained as historical evidence</Text>}</figcaption></figure>)}
  </Stack></section>;
}
