import { useState } from 'react';
import { Alert, Button, Group, Modal, Stack, Text, TextInput } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { deleteProject, type Project } from '@sceneproof/api-client';

export function DeleteProject({ project }: { project: Project }) {
  const [opened, setOpened] = useState(false);
  const [name, setName] = useState('');
  const navigate = useNavigate();
  const cache = useQueryClient();
  const deletion = useMutation({ mutationFn: () => deleteProject(project.id, name), onSuccess: async () => {
    try { sessionStorage.removeItem(`sceneproof.film-request.v1.${project.id}`); } catch { }
    cache.removeQueries({ predicate: query => query.queryKey.includes(project.id) });
    await cache.invalidateQueries({ queryKey: ['projects'] });
    navigate('/', { replace: true, state: { deletedProject: project.name } });
  } });
  if (project.demo) return null;
  return <><Button variant="subtle" color="red" size="compact-sm" onClick={() => { setName(''); deletion.reset(); setOpened(true); }}>Delete project</Button>
    <Modal opened={opened} onClose={() => { if (!deletion.isPending) setOpened(false); }} closeOnEscape={!deletion.isPending} closeOnClickOutside={!deletion.isPending} withCloseButton={!deletion.isPending} title={`Delete ${project.name}?`}>
      <Stack><Text>This is irreversible. Delete “{project.name}” and its uploaded media, Film Understanding history, transcripts, candidates, references and findings.</Text><Text size="sm" c="dimmed">Packaged demo and master assets are preserved. Active analysis must finish before deletion.</Text>
        <TextInput label="Type the project name to confirm" value={name} onChange={event => setName(event.currentTarget.value)} disabled={deletion.isPending} autoComplete="off" />
        <Group justify="end"><Button data-autofocus variant="default" disabled={deletion.isPending} onClick={() => setOpened(false)}>Keep project</Button><Button color="red" disabled={name !== project.name} loading={deletion.isPending} onClick={() => deletion.mutate()}>Delete permanently</Button></Group>
        {deletion.error && <Alert color="red" role="alert">{deletion.error.message}</Alert>}
      </Stack>
    </Modal></>;
}
