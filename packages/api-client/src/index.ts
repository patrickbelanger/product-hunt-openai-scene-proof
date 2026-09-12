import createClient from 'openapi-fetch';
import type { components, paths } from './schema';

export type Project = components['schemas']['Project'];
export type CreateProject = components['schemas']['CreateProject'];
export type ProjectPage = components['schemas']['ProjectPage'];
export type Problem = components['schemas']['Problem'];
export type Shot = components['schemas']['Shot'];
export type Frame = components['schemas']['Frame'];

export const api = createClient<paths>({ baseUrl: '/' });

export async function listShots(projectId: string, signal?: AbortSignal): Promise<Shot[]> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/shots', { params: { path: { projectId } }, signal });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function uploadShot(projectId: string, file: File): Promise<Shot> {
  const { data, error, response } = await api.POST('/api/v1/projects/{projectId}/shots', {
    params: { path: { projectId } }, body: { file: '' },
    bodySerializer: () => { const form = new FormData(); form.append('file', file); return form; },
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export class ApiError extends Error {
  constructor(public readonly status: number, problem?: Problem) {
    super(problem?.detail ?? 'SceneProof could not complete this request. Please try again.');
  }
}

export async function listProjects(page = 0, signal?: AbortSignal): Promise<ProjectPage> {
  const { data, error, response } = await api.GET('/api/v1/projects', {
    params: { query: { page } }, signal,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function getProject(id: string, signal?: AbortSignal): Promise<Project> {
  const { data, error, response } = await api.GET('/api/v1/projects/{id}', {
    params: { path: { id } }, signal,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function createProject(body: CreateProject): Promise<Project> {
  const { data, error, response } = await api.POST('/api/v1/projects', { body });
  if (!data) throw new ApiError(response.status, error);
  return data;
}
