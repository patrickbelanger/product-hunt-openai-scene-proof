import { useEffect, useRef, useState } from 'react';
import { Button, Group, Modal, Paper, Stack, Text } from '@mantine/core';

export const TOUR_PREFERENCE_KEY = 'sceneproof.guided-tour.v1';

const steps = [
  { target: 'reference-bible', title: 'Reference Bible', location: 'Reference Bible panel', text: 'Define what should stay consistent. Continuity rules and visual references are your source of truth. Editing here never starts analysis.' },
  { target: 'media', title: 'Media and timeline', location: 'Media workspace and shot timeline', text: 'Import stills or video, then inspect the ordered shots and representative frames used as evidence. The timeline follows your sequence.' },
  { target: 'findings', title: 'Findings and evidence', location: 'Continuity findings panel', text: 'After an analysis, any saved concerns appear here. Compare their evidence and copy an actionable correction. A difference alone is not a continuity error.' },
  { target: 'findings', title: 'Resolve and steer', location: 'Continuity findings panel · open finding actions', text: 'On an open finding, Resolve records a correction; Dismiss sets a concern aside. “This change is intentional” requests independent targeted re-evaluation. Creator intent adds context. It does not force Astra to agree.' },
] as const;

function shouldInvite() {
  try {
    const preference = localStorage.getItem(TOUR_PREFERENCE_KEY);
    return preference !== 'completed' && preference !== 'skipped';
  } catch {
    return false;
  }
}

export function GuidedTour({ onStart }: { onStart?: () => void } = {}) {
  const [invitation, setInvitation] = useState(shouldInvite);
  const [step, setStep] = useState<number | null>(null);
  const [missingTarget, setMissingTarget] = useState(false);
  const restart = useRef<HTMLButtonElement>(null);
  const opener = useRef<HTMLButtonElement | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const previousStep = useRef<number | null>(null);
  const current = step === null ? undefined : steps[step];

  function close(preference: 'completed' | 'skipped') {
    try { localStorage.setItem(TOUR_PREFERENCE_KEY, preference); } catch {}
    setInvitation(false);
    setStep(null);
    requestAnimationFrame(() => (opener.current?.isConnected ? opener.current : restart.current)?.focus());
  }

  function start(button: HTMLButtonElement) {
    opener.current = button;
    onStart?.();
    setStep(0);
  }

  useEffect(() => {
    const changedStep = previousStep.current !== null && previousStep.current !== step;
    previousStep.current = step;
    if (!current) return;
    const target = document.querySelector<HTMLElement>(`[data-tour="${current.target}"]`);
    function updateTarget() {
      const visible = !!target?.getClientRects().length && getComputedStyle(target).visibility !== 'hidden';
      setMissingTarget(!visible);
      if (visible) target!.dataset.tourActive = String(step! + 1);
      else target?.removeAttribute('data-tour-active');
      return visible;
    }
    if (updateTarget() && target) {
      const bounds = target.getBoundingClientRect();
      if (bounds.top < 24 || bounds.top > window.innerHeight / 2) target.scrollIntoView({ block: 'start', behavior: 'instant' });
    }
    if (changedStep) heading.current?.focus({ preventScroll: true });
    window.addEventListener('resize', updateTarget);
    return () => {
      target?.removeAttribute('data-tour-active');
      window.removeEventListener('resize', updateTarget);
    };
  }, [current, step]);

  return <div className="tour-controls" data-tour-open={step !== null || undefined}>
    <Button ref={restart} size="compact-sm" variant="subtle" onClick={event => start(event.currentTarget)}>Quick tour</Button>
    {invitation && <Paper component="aside" aria-label="Quick tour invitation" className="tour-invitation" withBorder p="sm">
      <Text size="sm">Take the quick tour</Text>
      <Group gap="xs" mt="xs"><Button size="compact-sm" variant="light" onClick={event => start(event.currentTarget)}>Start tour</Button><Button size="compact-sm" variant="subtle" onClick={() => { opener.current = null; close('skipped'); }}>Skip</Button></Group>
    </Paper>}
    <Modal.Root opened={step !== null} onClose={() => close('skipped')} returnFocus={false} lockScroll={false} closeOnClickOutside={false} transitionProps={{ duration: 0 }} size={400} classNames={{ inner: 'tour-modal-inner', content: 'tour-modal-content' }}>
      <Modal.Overlay backgroundOpacity={0.22} />
      <Modal.Content>
      <Modal.Header><Modal.Title ref={heading} tabIndex={-1} data-autofocus><Text component="span" display="block" size="xs" c="dimmed" mb={4}>Step {(step ?? 0) + 1} of 4</Text>{current?.title}</Modal.Title></Modal.Header>
      <Modal.Body>
      {current && <Stack gap="sm">
        <Text size="xs" c="teal.2">{current.location}</Text>
        <Text size="sm">{current.text}</Text>
        {missingTarget && <Text size="xs" c="dimmed">This area is not visible in the current layout. You can continue the tour or return to the workspace.</Text>}
        <Group justify="space-between" mt="xs"><Button variant="subtle" size="sm" onClick={() => close('skipped')}>Skip</Button><Group gap="xs">
          {step! > 0 && <Button variant="default" size="sm" onClick={() => setStep(step! - 1)}>Back</Button>}
          {step === 3 ? <Button size="sm" onClick={() => close('completed')}>Done</Button> : <Button size="sm" onClick={() => setStep(step! + 1)}>Next</Button>}
        </Group></Group>
      </Stack>}
      </Modal.Body>
      </Modal.Content>
    </Modal.Root>
  </div>;
}
