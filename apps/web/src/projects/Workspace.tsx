import { Alert, Anchor, Badge, Button, Group, Loader, Stack, Tabs, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, getProject } from '@sceneproof/api-client';
import { FindingsWorkspace } from './FindingsWorkspace';
import { ReferenceBible } from './ReferenceBible';
import { GuidedTour } from './GuidedTour';
import { DemoReset } from './DemoControls';
import { FilmIntelligence } from './FilmIntelligence';
import { DeleteProject } from './DeleteProject';

export function Workspace() {
  const { projectId = '' } = useParams();
  const [params] = useSearchParams();
  const analysisId = params.get('analysisId');
  const finding = params.get('finding');
  const filmRun = params.get('filmRun');
  const initialTab = finding || analysisId ? 'findings' : 'film';
  const [tab, setTab] = useState({ projectId, value: initialTab });
  const previousLink = useRef({ projectId, analysisId, finding, filmRun });
  const activeTab = tab.projectId === projectId ? tab.value : initialTab;
  function selectTab(value: string | null) { if (value) setTab({ projectId, value }); }
  useLayoutEffect(() => {
    const previous = previousLink.current;
    if (previous.projectId !== projectId) setTab({ projectId, value: finding || analysisId ? 'findings' : 'film' });
    else if (filmRun && previous.filmRun !== filmRun) setTab({ projectId, value: 'film' });
    else if ((finding && previous.finding !== finding) || (analysisId && previous.analysisId !== analysisId)) setTab({ projectId, value: 'findings' });
    previousLink.current = { projectId, analysisId, finding, filmRun };
  }, [projectId, analysisId, finding, filmRun]);
  const project = useQuery({ queryKey: ['project', projectId], queryFn: ({ signal }) => getProject(projectId, signal), retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 1 });
  const heading = useRef<HTMLHeadingElement>(null);
  const isDemo = Boolean(project.data?.demo);
  useEffect(() => { if (isDemo) heading.current?.focus(); }, [projectId, isDemo]);
  if (project.isPending) return <main id="main" className="page" role="status"><Group><Loader size="sm" /><Text>Opening your workspace…</Text></Group></main>;
  if (project.isError) return <main id="main" className="page narrow"><Alert color="red" title={project.error instanceof ApiError && project.error.status === 404 ? 'Project not found' : 'Could not open this project'} role="alert"><Stack><Text size="sm">Return to your projects, or try opening this workspace again.</Text><Group><Button component={Link} to="/" variant="default">All projects</Button><Button variant="light" color="red" onClick={() => void project.refetch()}>Try again</Button></Group></Stack></Alert></main>;
  const current = project.data;
  return <main id="main" className="workspace">
    <div className="workspace-toolbar"><div><Anchor component={Link} to="/" c="dimmed" size="xs">← All projects</Anchor><Title ref={heading} tabIndex={-1} order={1} size="h3" mt={4}>{current.name}</Title></div><Group><Badge color="teal" variant="light">{current.demo ? current.demo.retired ? 'Previous demo copy' : 'Demo project' : 'Project saved'}</Badge><DemoReset key={`reset-${projectId}`} project={current} /><GuidedTour key={`tour-${projectId}`} onNavigate={selectTab} /><DeleteProject key={`delete-${projectId}`} project={current} /></Group></div>
    {current.demo && <Text size="sm" c="dimmed" px="md" pb="md">{current.description} Explore the Reference Bible and select frames in the timeline. The demo starts without recorded findings; saved findings come from an explicitly requested analysis.</Text>}
    <Tabs value={activeTab} onChange={selectTab} keepMounted className="workspace-tabs">
      <Tabs.List aria-label="Workspace workflow" grow><Tabs.Tab value="film">Film Intelligence</Tabs.Tab><Tabs.Tab value="findings">Continuity Findings</Tabs.Tab></Tabs.List>
      <Tabs.Panel value="film">
        <Text size="sm" c="dimmed" p="md">Understand the film's visual and narrative context, then confirm what should stay consistent.</Text>
        <FilmIntelligence key={`film-${projectId}`} projectId={projectId} />
      </Tabs.Panel>
      <Tabs.Panel value="findings">
        <Text size="sm" c="dimmed" p="md">Inspect frame-level continuity issues, compare supporting evidence and review what needs attention.</Text>
        <div className="workspace-grid">
          <ReferenceBible key={`bible-${projectId}`} project={current} />
          <FindingsWorkspace key={projectId} projectId={projectId} demo={isDemo} active={activeTab === 'findings'} />
        </div>
      </Tabs.Panel>
    </Tabs>
  </main>;
}
