import { useRef, useState } from 'react';
import { Alert, Button, FileInput, Group, Modal, Stack, Text, Textarea, TextInput, Title } from '@mantine/core';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { archiveReference, listReferences, updateProjectRules, updateReference, uploadReference, type Project, type Reference } from '@sceneproof/api-client';
import { ReferenceImage } from './ReferenceImage';

function RulesEditor({ project }: { project: Project }) {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState<string>();
  const rules = draft ?? project.rules;
  const save = useMutation({ mutationFn: () => updateProjectRules(project.id, { rules }), onSuccess: saved => {
    queryClient.setQueryData(['project', project.id], saved);
    setDraft(undefined);
  } });
  return <form onSubmit={event => { event.preventDefault(); save.mutate(); }}>
    <Textarea label="Continuity rules" value={rules} onChange={event => { setDraft(event.currentTarget.value); save.reset(); }} maxLength={8000} autosize minRows={4} maxRows={10} disabled={save.isPending} description="What should stay consistent? Optional · 8000 characters." />
    <Button type="submit" size="xs" mt="sm" variant="light" loading={save.isPending} disabled={rules === project.rules}>Save rules</Button>
    {save.isPending && <Text size="xs" role="status" mt="xs">Saving rules…</Text>}
    {save.isSuccess && <Text size="xs" c="teal.3" role="status" mt="xs">Rules saved.</Text>}
    {save.isError && <Alert color="red" title="Rules not saved" role="alert" mt="sm">{save.error.message}</Alert>}
  </form>;
}

function ReferenceEditor({ projectId, reference, close }: { projectId: string; reference?: Reference; close: () => void }) {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState(reference?.title ?? '');
  const [guidance, setGuidance] = useState(reference?.guidance ?? '');
  const [file, setFile] = useState<File | null>(null);
  const [validation, setValidation] = useState('');
  const [confirmArchive, setConfirmArchive] = useState(false);
  function updateCache(saved: Reference) {
    queryClient.setQueryData<Reference[]>(['references', projectId], previous => {
      const remaining = (previous ?? []).filter(item => item.id !== saved.id);
      return saved.archivedAt ? remaining : [...remaining, saved].sort((first, second) => first.createdAt.localeCompare(second.createdAt) || first.id.localeCompare(second.id));
    });
    void queryClient.invalidateQueries({ queryKey: ['finding-references', projectId] });
    close();
  }
  const save = useMutation({ mutationFn: () => reference ? updateReference(projectId, reference.id, { title, guidance }) : uploadReference(projectId, file!, { title, guidance }), onSuccess: updateCache });
  const archive = useMutation({ mutationFn: () => archiveReference(projectId, reference!.id), onSuccess: updateCache });
  const pending = save.isPending || archive.isPending;
  function submit() {
    setValidation('');
    if (!title.trim()) { setValidation('Give this reference a title.'); return; }
    if (!reference && (!file || file.size === 0 || file.size > 10 * 1024 * 1024 || !['image/jpeg', 'image/png'].includes(file.type))) {
      setValidation('Choose a JPEG or PNG image up to 10 MiB.'); return;
    }
    save.mutate();
  }
  return <Modal opened onClose={close} title={reference ? 'Inspect visual reference' : 'Add visual reference'} centered returnFocus={false} closeOnClickOutside={!pending} closeOnEscape={!pending} withCloseButton={!pending}>
    <form onSubmit={event => { event.preventDefault(); submit(); }}><Stack>
      {reference ? <><ReferenceImage reference={reference} /><Text size="xs" c="dimmed">Image preserved. To replace it, archive this reference and add a new image. Earlier analyses keep their original evidence.</Text></>
        : <FileInput label="Reference image" description="JPEG / PNG · up to 10 MiB" accept="image/jpeg,image/png" value={file} onChange={setFile} disabled={pending} />}
      <TextInput label="Reference title" value={title} onChange={event => setTitle(event.currentTarget.value)} maxLength={120} disabled={pending} data-autofocus />
      <Textarea label="Creator guidance" description="What does this image establish? Optional." placeholder="Keep the face, hairstyle and black blazer consistent." value={guidance} onChange={event => setGuidance(event.currentTarget.value)} maxLength={2000} autosize minRows={3} maxRows={8} disabled={pending} />
      {validation && <Text c="red.3" size="sm" role="alert">{validation}</Text>}
      {save.isError && <Alert color="red" title="Reference not saved" role="alert">{save.error.message}</Alert>}
      {archive.isError && <Alert color="red" title="Reference not archived" role="alert">{archive.error.message}</Alert>}
      {pending && <Text size="sm" role="status">{archive.isPending ? 'Archiving reference…' : reference ? 'Saving reference…' : 'Uploading and validating reference…'}</Text>}
      <Group><Button type="submit" loading={save.isPending} disabled={archive.isPending}>Save reference</Button><Button variant="default" onClick={close} disabled={pending}>Cancel</Button></Group>
      {reference && <><Button color="gray" variant="subtle" disabled={pending} onClick={() => setConfirmArchive(true)}>Archive reference</Button>
        {confirmArchive && <Alert color="yellow" title="Remove from the active Bible?">The image stays available to earlier findings. This cannot be undone.<Button mt="sm" color="yellow" variant="light" loading={archive.isPending} disabled={save.isPending} onClick={() => archive.mutate()}>Confirm archive</Button></Alert>}</>}
    </Stack></form>
  </Modal>;
}

export function ReferenceBible({ project }: { project: Project }) {
  const references = useQuery({ queryKey: ['references', project.id], queryFn: ({ signal }) => listReferences(project.id, signal), retry: false });
  const [editor, setEditor] = useState<Reference | 'new'>();
  const opener = useRef<HTMLButtonElement | null>(null);
  const addButton = useRef<HTMLButtonElement | null>(null);
  function closeEditor() {
    setEditor(undefined);
    requestAnimationFrame(() => (opener.current?.isConnected ? opener.current : addButton.current)?.focus());
  }
  return <aside data-tour="reference-bible" className="workspace-panel reference-panel" aria-labelledby="reference-title"><Stack gap="lg">
    <Text className="panel-label" id="reference-title">REFERENCE BIBLE</Text>
    <Text size="xs" c="dimmed">Your declared visual and textual truth. Context for independent judgement.</Text>
    <RulesEditor project={project} />
    <section aria-labelledby="visual-references-title"><Title order={2} size="h5" id="visual-references-title">Visual references</Title>
      <Text size="xs" c="dimmed" mt="xs">Up to 8 active images. Optional. All active references are available to the next sequence analysis.</Text>
      {references.isPending && <Text role="status" size="sm" mt="sm">Loading references…</Text>}
      {references.isError && <Alert color="red" title="References unavailable" role="alert" mt="sm"><Text size="sm">{references.error.message}</Text><Button variant="light" size="xs" mt="sm" onClick={() => void references.refetch()}>Retry references</Button></Alert>}
      {references.isSuccess && references.data.length === 0 && <Text size="sm" c="dimmed" mt="sm">No visual references yet.</Text>}
      <ul className="reference-list">{references.data?.map(reference => <li key={reference.id}><button className="reference-card" type="button" onClick={event => { opener.current = event.currentTarget; setEditor(reference); }} aria-label={`Inspect reference: ${reference.title}`}><ReferenceImage reference={reference} /><strong>{reference.title}</strong><span>{reference.guidance || 'No creator guidance supplied.'}</span></button></li>)}</ul>
      <Button ref={addButton} size="xs" variant="light" fullWidth disabled={!references.isSuccess || references.data.length >= 8} onClick={event => { opener.current = event.currentTarget; setEditor('new'); }}>+ Add visual reference</Button>
    </section>
    <Text size="xs" c="dimmed">Editing the Bible never starts analysis.</Text>
    {project.description && <div><Text className="panel-label">PROJECT NOTES</Text><Text size="sm" c="dimmed" mt="sm" className="rules-text">{project.description}</Text></div>}
    {editor && <ReferenceEditor projectId={project.id} reference={editor === 'new' ? undefined : editor} close={closeEditor} />}
  </Stack></aside>;
}
