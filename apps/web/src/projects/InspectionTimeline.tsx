import { useLayoutEffect, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { Button, Group, Text } from '@mantine/core';
import type { FilmIntelligence, Shot } from '@sceneproof/api-client';

export function InspectionTimeline({ shots, segments, selectedId, onSelect, children, active = true }: { shots: Shot[]; segments: FilmIntelligence['segments']; selectedId?: string; onSelect: (id: string) => void; children: ReactNode; active?: boolean }) {
  const root = useRef<HTMLDivElement>(null);
  const frames = shots.flatMap(shot => {
    const segment = segments.find(value => value.id === shot.id);
    return shot.frames.map(frame => ({ ...frame, shotId: shot.id, group: segment ? 'source' : shot.id, time: frame.timestampMs === null ? null : (segment?.startMs ?? 0) + frame.timestampMs }));
  });
  const index = Math.max(0, frames.findIndex(frame => frame.id === selectedId));
  const current = frames[index];
  useLayoutEffect(() => {
    if (!active || !current) return;
    const viewport = root.current?.querySelector<HTMLElement>('.media-timeline');
    const frame = document.getElementById(`frame-${current.id}`);
    if (!viewport || !frame || !viewport.contains(frame)) return;
    const bounds = viewport.getBoundingClientRect();
    if (!bounds.width || !bounds.height) return;
    const selected = frame.getBoundingClientRect();
    const top = bounds.top + viewport.clientTop;
    const left = bounds.left + viewport.clientLeft;
    const vertical = selected.top < top ? selected.top - top : Math.max(0, selected.bottom - top - viewport.clientHeight);
    const horizontal = selected.left < left ? selected.left - left : Math.max(0, selected.right - left - viewport.clientWidth);
    if (vertical || horizontal) viewport.scrollBy({ top: vertical, left: horizontal, behavior: 'instant' });
  }, [current?.id, active]);
  const [range, setRange] = useState<{ group: string; start?: number; end?: number }>();
  const [notice, setNotice] = useState('');
  const visibleRange = range?.group === current?.group ? range : undefined;
  const timelineFrames = frames.filter(frame => frame.group === current?.group && frame.time !== null);
  const minimum = timelineFrames[0]?.time ?? 0;
  const maximum = timelineFrames.at(-1)?.time ?? minimum;
  const position = (time: number) => maximum === minimum ? 0 : (time - minimum) / (maximum - minimum) * 100;
  const stamp = (time: number) => `${(time / 1000).toFixed(2)}s`;
  function mark(boundary: 'start' | 'end') {
    if (!current || current.time === null) return;
    const next = { ...visibleRange, group: current.group, [boundary]: current.time };
    if (next.start !== undefined && next.end !== undefined && next.start > next.end) {
      setNotice('Mark In must be before or at Mark Out. Adjust the other mark or clear the range.');
      return;
    }
    setNotice(''); setRange(next);
  }
  function clear() { setRange(undefined); setNotice('Selection cleared.'); }
  function navigate(event: KeyboardEvent<HTMLDivElement>) {
    const target = event.target as HTMLElement;
    if (event.defaultPrevented || event.ctrlKey || event.metaKey || event.altKey || target.closest('input, textarea, select, [contenteditable=true], [role=textbox], [role=combobox], [role=slider]')) return;
    const key = event.key.toLowerCase();
    if (!['arrowleft', 'arrowright', 'i', 'o', 'escape', 'home', 'end'].includes(key)) return;
    event.preventDefault();
    if (key === 'i') { mark('start'); return; }
    if (key === 'o') { mark('end'); return; }
    if (key === 'escape') { clear(); return; }
    let next = index;
    if (key === 'home') next = 0;
    else if (key === 'end') next = frames.length - 1;
    else {
      const direction = key === 'arrowleft' ? -1 : 1;
      next = index + direction;
      if (event.shiftKey) {
        while (frames[next]?.shotId === current?.shotId) next += direction;
        if (direction < 0 && frames[next]) {
          const previousShot = frames[next]!.shotId;
          while (frames[next - 1]?.shotId === previousShot) next--;
        }
      }
    }
    const selected = frames[Math.max(0, Math.min(frames.length - 1, next))];
    if (selected) onSelect(selected.id);
  }
  return <div ref={root} className="inspection-timeline" tabIndex={0} role="region" aria-label="Keyboard timeline inspection" aria-describedby="timeline-shortcuts" onKeyDown={navigate}>
    <Group justify="space-between"><Text size="sm" fw={600} aria-live="polite">Playhead · {current?.time === null ? 'Still image' : current ? `${current.group === 'source' ? 'Source' : 'Clip'} ${stamp(current.time!)}` : 'No sampled frame'}</Text><Text size="xs">Frame {frames.length ? index + 1 : 0} of {frames.length}</Text></Group>
    <Text id="timeline-shortcuts" size="xs" c="dimmed" mt="xs">← → Frame · Shift+← → Segment · I Mark In · O Mark Out · Esc Clear · Home/End</Text>
    <Text size="xs" c="dimmed">Sampled-frame inspection; video playback is not available.</Text>
    {timelineFrames.length > 0 && <div className="inspection-rail" aria-label="Inspection range">
      {visibleRange?.start !== undefined && visibleRange.end !== undefined && <span className="inspection-range" data-testid="inspection-range" style={{ left: `${position(visibleRange.start)}%`, width: `${position(visibleRange.end) - position(visibleRange.start)}%` }} />}
      {timelineFrames.map(frame => <button key={frame.id} type="button" className="inspection-tick" style={{ left: `${position(frame.time!)}%` }} aria-label={`Inspect ${stamp(frame.time!)}`} aria-current={frame.id === current?.id ? 'true' : undefined} onClick={() => onSelect(frame.id)} />)}
      {current?.time !== null && <span className="inspection-playhead" aria-hidden="true" style={{ left: `${position(current!.time!)}%` }}>▼</span>}
      {visibleRange?.start !== undefined && <button type="button" className="inspection-handle in" style={{ left: `${position(visibleRange.start)}%` }} onClick={() => { const frame = timelineFrames.find(value => value.time === visibleRange.start); if (frame) onSelect(frame.id); }} aria-label={`Go to Mark In ${stamp(visibleRange.start)}`}>In</button>}
      {visibleRange?.end !== undefined && <button type="button" className="inspection-handle out" style={{ left: `${position(visibleRange.end)}%` }} onClick={() => { const frame = timelineFrames.find(value => value.time === visibleRange.end); if (frame) onSelect(frame.id); }} aria-label={`Go to Mark Out ${stamp(visibleRange.end)}`}>Out</button>}
    </div>}
    <Group gap="xs" mt="sm"><Button size="compact-sm" variant="default" disabled={!current || current.time === null} onClick={() => mark('start')}>Mark In</Button><Button size="compact-sm" variant="default" disabled={!current || current.time === null} onClick={() => mark('end')}>Mark Out</Button><Button size="compact-sm" variant="subtle" disabled={!range} onClick={clear}>Clear range</Button></Group>
    <Text size="xs" mt="xs">In {visibleRange?.start === undefined ? '—' : stamp(visibleRange.start)} · Out {visibleRange?.end === undefined ? '—' : stamp(visibleRange.end)} · Selection {visibleRange?.start !== undefined && visibleRange.end !== undefined ? stamp(visibleRange.end - visibleRange.start) : '—'}</Text>
    <Text size="xs" c="dimmed">Inspection only · marks apply within this {current?.group === 'source' ? 'analysis source' : 'clip'} and do not change AI analysis.</Text>
    {notice && <Text size="xs" role="status">{notice}</Text>}
    {children}
  </div>;
}
