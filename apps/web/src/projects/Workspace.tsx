import { Alert, Anchor, Badge, Button, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getProject } from '@sceneproof/api-client';

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
      <section className="viewer-panel" aria-labelledby="viewer-title"><div className="viewer-meta"><span>FRAME INSPECTOR</span><span>NO SHOT SELECTED</span></div><div className="empty-viewfinder"><svg width="56" height="44" viewBox="0 0 56 44" fill="none" aria-hidden="true"><path d="M15 1H1v12M41 1h14v12M1 31v12h14M55 31v12h-14" stroke="currentColor" /><path d="M22 15v14l13-7-13-7Z" stroke="currentColor" /></svg><Title id="viewer-title" order={2} size="h3">Your sequence belongs here.</Title><Text size="sm" c="dimmed" maw={330} ta="center">Your project is saved. Shot import and continuity analysis are the next steps in SceneProof’s development.</Text></div><div className="viewer-meta"><span>REFERENCE → SEQUENCE → REVIEW</span><span>— / —</span></div></section>
      <aside className="workspace-panel findings-panel" aria-labelledby="findings-title"><Group justify="space-between"><Text className="panel-label" id="findings-title">CONTINUITY FINDINGS</Text><Badge color="gray" size="sm">0</Badge></Group><div className="findings-empty"><span className="proof-mark" aria-hidden="true">◎</span><Title order={2} size="h4">Nothing reviewed yet.</Title><Text size="sm" c="dimmed" mt="sm">Findings will appear here after your shots have been analyzed.</Text></div></aside>
    </div>
    <section className="timeline-panel" aria-labelledby="timeline-title"><Group justify="space-between"><Text className="panel-label" id="timeline-title">SHOT TIMELINE</Text><Text size="xs" c="dimmed">0 SHOTS</Text></Group><div className="timeline-empty"><span aria-hidden="true">01</span><Text size="sm" c="dimmed">No shots in this sequence.</Text><div className="timeline-track" /></div></section>
  </main>;
}
