import { Alert, Anchor, Badge, Button, Group, Loader, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getProject } from '@sceneproof/api-client';
import { FindingsWorkspace } from './FindingsWorkspace';
import { ReferenceBible } from './ReferenceBible';
import { GuidedTour } from './GuidedTour';
import { DemoReset } from './DemoControls';

export function Workspace() {
  const { projectId = '' } = useParams();
  const project = useQuery({ queryKey: ['project', projectId], queryFn: ({ signal }) => getProject(projectId, signal), retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 1 });
  const heading = useRef<HTMLHeadingElement>(null);
  const isDemo = Boolean(project.data?.demo);
  useEffect(() => { if (isDemo) heading.current?.focus(); }, [projectId, isDemo]);
  if (project.isPending) return <main id="main" className="page" role="status"><Group><Loader size="sm" /><Text>Opening your workspace…</Text></Group></main>;
  if (project.isError) return <main id="main" className="page narrow"><Alert color="red" title={project.error instanceof ApiError && project.error.status === 404 ? 'Project not found' : 'Could not open this project'} role="alert"><Stack><Text size="sm">Return to your projects, or try opening this workspace again.</Text><Group><Button component={Link} to="/" variant="default">All projects</Button><Button variant="light" color="red" onClick={() => void project.refetch()}>Try again</Button></Group></Stack></Alert></main>;
  const current = project.data;
  return <main id="main" className="workspace">
    <div className="workspace-toolbar"><div><Anchor component={Link} to="/" c="dimmed" size="xs">← All projects</Anchor><Title ref={heading} tabIndex={-1} order={1} size="h3" mt={4}>{current.name}</Title></div><Group><Badge color="teal" variant="light">{current.demo ? current.demo.retired ? 'Previous demo copy' : 'Demo project' : 'Project saved'}</Badge><DemoReset key={`reset-${projectId}`} project={current} /><GuidedTour key={`tour-${projectId}`} /></Group></div>
    {current.demo && <Text size="sm" c="dimmed" px="md" pb="md">{current.description} Explore the Reference Bible and select frames in the timeline. The demo starts without recorded findings; saved findings come from an explicitly requested analysis.</Text>}
    <div className="workspace-grid">
      <ReferenceBible key={`bible-${projectId}`} project={current} />
      <FindingsWorkspace key={projectId} projectId={projectId} demo={isDemo} />
    </div>
  </main>;
}
