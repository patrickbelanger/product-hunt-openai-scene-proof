import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MantineProvider } from '@mantine/core';
import { expect, it } from 'vitest';
import manifest from '../../../../demo/runtime/manifest.json';
import { SourceProvenance } from './SourceProvenance';

const source = { id: 'source', projectId: 'project', name: 'Film', sha256: manifest.sourceFilm.sha256, byteSize: 100, durationMs: 36292, createdAt: '' };
it('labels the current derivative and exposes verified master/range provenance by hash', async () => {
  render(<MantineProvider><SourceProvenance source={source} /></MantineProvider>);
  expect(screen.getByText('Analysis source · 36.29s')).toBeVisible();
  expect(screen.getByText('Film')).toBeVisible();
  expect(screen.getByText('Source fixed for this project.')).toBeVisible();
  await userEvent.click(screen.getByText('Source details'));
  expect(screen.getByText('Master · 92.46s · Analysis range · 0.00–36.29s')).toBeVisible();
  expect(screen.getByText('Source identity: source')).toBeVisible();
  expect(screen.getByText(/Source replacement is not available/)).toBeVisible();
});
it('preserves historical master duration and does not infer derivative provenance for unknown uploads', () => {
  render(<MantineProvider><SourceProvenance source={{ ...source, durationMs: 92459, sha256: manifest.sourceProvenance.masterSha256 }} /><SourceProvenance source={{ ...source, durationMs: 4000, sha256: 'unknown' }} /></MantineProvider>);
  expect(screen.getByText('Analysis source · 92.46s')).toBeVisible();
  expect(screen.getByText('Analysis source · 4.00s')).toBeVisible();
  expect(screen.queryByText(/Analysis range/)).not.toBeInTheDocument();
});
