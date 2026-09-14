import { Alert, Badge, Button, Group, Loader, Paper, Stack, Text, Title } from '@mantine/core';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { listProjects } from '@sceneproof/api-client';
import { DemoLaunch } from './DemoControls';

export function ProjectLibrary() {
  const [page, setPage] = useState(0);
  const projects = useQuery({ queryKey: ['projects', page], queryFn: ({ signal }) => listProjects(page, signal) });
  return <main id="main" className="page library">
    <section className="hero">
      <div>
        <Text className="eyebrow" c="teal.3">YOUR AI CONTINUITY SUPERVISOR</Text>
        <Title order={1}>Make your film<br /><span>remember itself.</span></Title>
        <Text c="gray.4" className="hero-copy">Your characters. Your world. Every detail.<br />Give your next sequence a place to stay consistent.</Text>
        <Group align="flex-start"><DemoLaunch /><Button component={Link} to="/projects/new" size="md" variant="default">Create a project <span aria-hidden="true" className="button-arrow">↗</span></Button></Group>
      </div>
      <div className="film-diagram" aria-label="Reference, sequence, review">
        <div className="diagram-label">THE CONTINUITY PROCESS</div>
        <div className="diagram-frame"><span className="frame-corner" /><span className="diagram-number">01 / REFERENCE</span><div className="frame-line" /><span className="diagram-subtitle">Define your world.</span></div>
        <div className="diagram-footer"><span>02 / SEQUENCE</span><span>03 / REVIEW</span></div>
      </div>
    </section>
    <section aria-labelledby="projects-title" className="project-section">
      <Group justify="space-between" mb="lg"><Title order={2} id="projects-title" size="h3">Your projects</Title><Badge color="gray" variant="light">PRODUCTION LIBRARY</Badge></Group>
      {projects.isPending && <Group role="status"><Loader size="sm" /><Text>Loading your projects…</Text></Group>}
      {projects.isError && <Alert color="red" title="Could not load your projects" role="alert"><Text size="sm">Check that SceneProof is running, then try again.</Text><Button variant="light" color="red" mt="sm" onClick={() => void projects.refetch()}>Try again</Button></Alert>}
      {projects.data && <>
        {projects.data.items.length === 0 ? <Paper className="library-empty" p="xl" withBorder><Stack align="center" gap="xs"><Text size="xl">A new sequence starts here.</Text><Text c="dimmed" ta="center">Create a project and write the continuity rules that define your film.</Text></Stack></Paper> : <div className="project-grid">{projects.data.items.map(project => <Paper key={project.id} className="project-card" withBorder p="lg">
          <div className="project-card-top"><span aria-hidden="true">▱</span><Text size="xs" c="dimmed">{new Date(project.createdAt).toLocaleDateString('en-CA')}</Text></div>
          <Title order={3} size="h4"><Link to={`/projects/${project.id}`}>{project.name}</Link></Title>
          <Text size="sm" c="dimmed" lineClamp={2} mt="sm">{project.description || 'Ready for your next sequence.'}</Text>
        </Paper>)}</div>}
        {(page > 0 || projects.data.hasNext) && <Group justify="flex-end" mt="lg"><Button variant="default" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</Button><Text size="sm">Page {page + 1}</Text><Button variant="default" disabled={!projects.data.hasNext} onClick={() => setPage(page + 1)}>Next</Button></Group>}
      </>}
    </section>
    <footer className="library-footer"><span>KEEP EVERY SHOT IN CHARACTER.</span><span>SceneProof / Foundation</span></footer>
  </main>;
}
