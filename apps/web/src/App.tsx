import { Anchor, Badge, Button, Group, Text } from '@mantine/core';
import { Link, Route, Routes } from 'react-router-dom';
import { ProjectLibrary } from './projects/ProjectLibrary';
import { NewProject } from './projects/NewProject';
import { Workspace } from './projects/Workspace';
import { PrivacyPolicy, TermsOfUse } from './legal/LegalPages';
import { repositoryUrl } from './legal/disclosures';
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

function ScrollToTop() {
  const { pathname } = useLocation();

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: 'auto' });
  }, [pathname]);

  return null;
}

export function App() {
  return <>
    <ScrollToTop />
    <a className="skip-link" href="#main">Skip to content</a>
    <header className="app-header">
      <Anchor component={Link} to="/" className="brand" underline="never" c="gray.1">
        <svg width="29" height="29" viewBox="0 0 32 32" fill="none" aria-hidden="true">
          <path d="M11 4H4v7M21 4h7v7M4 21v7h7M28 21v7h-7" stroke="currentColor" strokeWidth="2" />
          <path d="m10 16 4 4 8-9" stroke="#63dbc0" strokeWidth="2" />
        </svg>
        SceneProof
      </Anchor>
      <Group gap="lg"><Text size="xs" c="dimmed" className="header-caption">CONTINUITY WORKSPACE</Text><Badge variant="outline" color="gray">Local workspace</Badge></Group>
    </header>
    <Routes>
      <Route path="/" element={<ProjectLibrary />} />
      <Route path="/privacy" element={<PrivacyPolicy />} />
      <Route path="/terms" element={<TermsOfUse />} />
      <Route path="/projects/new" element={<NewProject />} />
      <Route path="/projects/:projectId" element={<Workspace />} />
      <Route path="*" element={<main id="main" className="page narrow"><h1>That page is out of frame.</h1><Button component={Link} to="/">Back to projects</Button></main>} />
    </Routes>
    <footer className="site-footer"><nav aria-label="Legal and source links">
      <Link to="/privacy">Privacy</Link>
      <Link to="/terms">Terms</Link>
      <a href={repositoryUrl}>GitHub</a>
    </nav></footer>
  </>;
}
