import { Alert, Anchor, Badge, Button, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getProject } from '@sceneproof/api-client';
import { MediaWorkspace } from './MediaWorkspace';

export function Workspace() {
  const { projectId = '' } = useParams();
  const project = useQuery({ queryKey: ['project', projectId], queryFn: ({ signal }) => getProject(projectId, signal), retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 1 });
  if (project.isPending) return <main id="main" className="page" role="status"><Group><Loader size="sm" /><Text>Opening your workspace…</Text></Group></main>;
  if (project.isError) return <main id="main" className="page narrow"><Alert color="red" title={project.error instanceof ApiError && project.error.status === 404 ? 'Project not found' : 'Could not open this project'} role="alert"><Stack><Text size="sm">Return to your projects, or try opening this workspace again.</Text><Group><Button component={Link} to="/" variant="default">All projects</Button><Button variant="light" color="red" onClick={() => void project.refetch()}>Try again</Button></Group></Stack></Alert></main>;
  const current = project.data;
  return <main id="main" className="workspace">
    <div className="workspace-toolbar"><div><Anchor component={Link} to="/" c="dimmed" size="xs">← All projects</Anchor><Title order={1} size="h3" mt={4}>{current.name}</Title></div><Badge color="teal" variant="light">Project saved</Badge></div>
    <div className="workspace-grid">
      <aside className="workspace-panel reference-panel" aria-labelledby="reference-title"><Text className="panel-label" id="reference-title">REFERENCE BIBLE</Text><Title order={2} size="h4" mt="xl">Your continuity rules</Title><Text size="sm" c="gray.4" mt="md" className="rules-text">{current.rules || 'No rules yet. This project is ready for its visual references.'}</Text>{current.description && <><Text className="panel-label" mt="xl">PROJECT NOTES</Text><Text size="sm" c="dimmed" mt="md" className="rules-text">{current.description}</Text></>}</aside>
      <MediaWorkspace key={projectId} projectId={projectId} />
      <aside className="workspace-panel findings-panel" aria-labelledby="findings-title"><Group justify="space-between"><Text className="panel-label" id="findings-title">CONTINUITY FINDINGS</Text><Badge color="gray" size="sm">0</Badge></Group><div className="findings-empty"><span className="proof-mark" aria-hidden="true">◎</span><Title order={2} size="h4">Nothing reviewed yet.</Title><Text size="sm" c="dimmed" mt="sm">Findings will appear here after your shots have been analyzed.</Text></div></aside>
    </div>
  </main>;
}
