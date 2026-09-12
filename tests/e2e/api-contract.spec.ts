import { expect, test } from '@playwright/test';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import spec from '../../packages/api-client/openapi.json';

const validator = new Ajv2020({ strict: false, allErrors: true });
addFormats(validator);
validator.addSchema(spec, 'sceneproof');

function validate(schema: string, payload: unknown) {
  const check = validator.compile({ $ref: `sceneproof#/components/schemas/${schema}` });
  expect(check(payload), JSON.stringify(check.errors)).toBe(true);
}

test('live API and generated contract agree on projects, pages and problems', async ({ request }) => {
  const published = await request.get('/openapi.json');
  expect(await published.json()).toEqual(spec);
  const created = await request.post('/api/v1/projects', {
    data: { name: 'Contract verification', rules: 'The phone stays in the right hand.' },
  });
  expect(created.status()).toBe(201);
  validate('Project', await created.json());
  const fetched = await request.get(created.headers().location);
  expect(fetched.status()).toBe(200);
  expect(await fetched.json()).toEqual(await created.json());
  const listed = await request.get('/api/v1/projects');
  validate('ProjectPage', await listed.json());
  const invalid = await request.post('/api/v1/projects', { data: { name: '  ' } });
  expect(invalid.status()).toBe(400);
  expect(invalid.headers()['content-type']).toContain('application/problem+json');
  validate('Problem', await invalid.json());
  const missing = await request.get('/api/v1/projects/00000000-0000-0000-0000-000000000000');
  expect(missing.status()).toBe(404);
  validate('Problem', await missing.json());
});
