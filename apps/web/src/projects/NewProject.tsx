import { Alert, Anchor, Button, Group, Paper, Stack, Text, Textarea, TextInput, Title } from '@mantine/core';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { createProject } from '@sceneproof/api-client';

export function NewProject() {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [rules, setRules] = useState('');
  const [nameError, setNameError] = useState('');
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const mutation = useMutation({ mutationFn: createProject, onSuccess: project => {
    void queryClient.invalidateQueries({ queryKey: ['projects'] });
    queryClient.setQueryData(['project', project.id], project);
    navigate(`/projects/${project.id}`);
  } });
  function submit(event: FormEvent) {
    event.preventDefault();
    if (mutation.isPending) return;
    if (!name.trim()) { setNameError('Give your project a name.'); return; }
    setNameError('');
    mutation.mutate({ name: name.trim(), description, rules });
  }
  return <main id="main" className="page narrow">
    <Anchor component={Link} to="/" c="dimmed" size="sm">← All projects</Anchor>
    <Text className="eyebrow" c="teal.3" mt="xl">SET THE SCENE</Text>
    <Title order={1} mt="sm">Create a project</Title>
    <Text c="dimmed" mt="sm" mb="xl">Start with the details your film should remember.</Text>
    <Paper withBorder p="xl"><form onSubmit={submit}><Stack gap="lg">
      <TextInput label="Project name" placeholder="Between the Line" required maxLength={120} value={name} error={nameError} onChange={event => setName(event.currentTarget.value)} autoFocus disabled={mutation.isPending} />
      <Textarea label="Description" description="Optional — a little context about your sequence." maxLength={2000} autosize minRows={2} value={description} onChange={event => setDescription(event.currentTarget.value)} disabled={mutation.isPending} />
      <Textarea label="Continuity rules" description="What must stay consistent? Include any changes you already intend." placeholder="Patrick wears a fitted black blazer throughout the metro sequence." maxLength={8000} autosize minRows={4} value={rules} onChange={event => setRules(event.currentTarget.value)} disabled={mutation.isPending} />
      {mutation.isError && <Alert color="red" title="Project not saved" role="alert">We could not save your project. Your details are still here; please try again.</Alert>}
      <Group justify="space-between"><Button component={Link} to="/" variant="subtle" color="gray" disabled={mutation.isPending}>Cancel</Button><Button type="submit" loading={mutation.isPending} color="teal.3" c="dark.9">Create project</Button></Group>
    </Stack></form></Paper>
  </main>;
}
