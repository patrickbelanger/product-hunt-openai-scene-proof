import { Alert, Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useQueryClient } from '@tanstack/react-query';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { openDemo, resetDemo, type Project } from '@sceneproof/api-client';

const storageKey = 'sceneproof.demo.instance.v1';

export function DemoLaunch() {
  const navigate = useNavigate();
  const cache = useQueryClient();
  const busy = useRef(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  async function launch() {
    if (busy.current) return;
    busy.current = true; setPending(true); setError('');
    try {
      let requestId = sessionStorage.getItem(storageKey);
      if (!requestId) {
        requestId = crypto.randomUUID();
        sessionStorage.setItem(storageKey, requestId);
      }
      const project = await openDemo(requestId);
      cache.setQueryData(['project', project.id], project);
      void cache.invalidateQueries({ queryKey: ['projects'] });
      navigate(`/projects/${project.id}`);
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Enable browser session storage and check that SceneProof is running, then retry.');
    } finally { busy.current = false; setPending(false); }
  }
  return <Stack gap="xs">
    <Button size="md" color="teal.3" c="dark.9" loading={pending} onClick={() => void launch()}>Try the demo film</Button>
    {pending && <Text size="sm" role="status">Preparing your demo copy…</Text>}
    {error && <Alert color="red" title="Could not open the demo" role="alert">{error} Retry with Try the demo film; your request is preserved.</Alert>}
  </Stack>;
}

export function DemoReset({ project }: { project: Project }) {
  const navigate = useNavigate();
  const cache = useQueryClient();
  const busy = useRef(false);
  const [opened, setOpened] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState('');
  if (!project.demo || project.demo.retired) return null;
  async function reset() {
    if (busy.current) return;
    busy.current = true; setPending(true); setError('');
    try {
      const fresh = await resetDemo(project.id);
      cache.setQueryData(['project', fresh.id], fresh);
      void cache.invalidateQueries({ queryKey: ['projects'] });
      void cache.invalidateQueries({ queryKey: ['project', project.id] });
      setOpened(false);
      navigate(`/projects/${fresh.id}`, { replace: true });
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Check that SceneProof is running, then retry.');
    } finally { busy.current = false; setPending(false); }
  }
  return <>
    <Button variant="subtle" size="xs" onClick={() => setOpened(true)}>Reset demo</Button>
    <Modal opened={opened} onClose={() => { if (!busy.current) setOpened(false); }} title="Reset this demo copy?" closeOnClickOutside={false} closeOnEscape={!pending} withCloseButton={!pending}>
      <Stack>
        <Text size="sm">Start again with the original rules, references and ordered footage. Your edits, uploads and findings will not carry into the new copy. Previous analysis and action history stay with the old copy. Other projects are unaffected.</Text>
        {pending && <Text role="status">Restoring the authored demo…</Text>}
        {error && <Alert color="red" title="Demo reset failed" role="alert">{error} Retry to recover the same reset.</Alert>}
        <Group justify="flex-end"><Button variant="default" disabled={pending} data-autofocus onClick={() => setOpened(false)}>Keep my changes</Button><Button color="red" loading={pending} onClick={() => void reset()}>Reset this copy</Button></Group>
      </Stack>
    </Modal>
  </>;
}
