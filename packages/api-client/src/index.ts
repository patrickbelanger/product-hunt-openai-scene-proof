import createClient from 'openapi-fetch';
import type { components, paths } from './schema';

export type Project = components['schemas']['Project'];
export type CreateProject = components['schemas']['CreateProject'];
export type ProjectPage = components['schemas']['ProjectPage'];
export type Problem = components['schemas']['Problem'];
export type Shot = components['schemas']['Shot'];
export type Frame = components['schemas']['Frame'];
export type AnalysisRun = components['schemas']['AnalysisRun'];
export type Finding = components['schemas']['Finding'];
export type FindingAction = components['schemas']['FindingAction'];
export type CreateFindingAction = components['schemas']['CreateFindingAction'];
export type Reference = components['schemas']['Reference'];
export type ReferenceMetadata = components['schemas']['ReferenceMetadata'];
export type UpdateRules = components['schemas']['UpdateRules'];

export async function updateProjectRules(id: string, body: UpdateRules): Promise<Project> {
  const { data, error, response } = await api.PUT('/api/v1/projects/{id}/rules', { params: { path: { id } }, body });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function listReferences(projectId: string, signal?: AbortSignal): Promise<Reference[]> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/references', { params: { path: { projectId } }, signal });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function getReference(projectId: string, referenceId: string, signal?: AbortSignal): Promise<Reference> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/references/{referenceId}', { params: { path: { projectId, referenceId } }, signal });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function uploadReference(projectId: string, file: File, metadata: ReferenceMetadata): Promise<Reference> {
  const { data, error, response } = await api.POST('/api/v1/projects/{projectId}/references', {
    params: { path: { projectId } }, body: { file: '', ...metadata },
    bodySerializer: () => { const form = new FormData(); form.append('file', file); form.append('title', metadata.title); form.append('guidance', metadata.guidance); return form; },
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function updateReference(projectId: string, referenceId: string, body: ReferenceMetadata): Promise<Reference> {
  const { data, error, response } = await api.PUT('/api/v1/projects/{projectId}/references/{referenceId}', { params: { path: { projectId, referenceId } }, body });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function archiveReference(projectId: string, referenceId: string): Promise<Reference> {
  const { data, error, response } = await api.POST('/api/v1/projects/{projectId}/references/{referenceId}/archive', { params: { path: { projectId, referenceId } } });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function listFindingReferences(projectId: string, findingId: string, signal?: AbortSignal): Promise<Reference[]> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/findings/{findingId}/references', { params: { path: { projectId, findingId } }, signal });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function createFindingAction(projectId: string, findingId: string, body: CreateFindingAction): Promise<FindingAction> {
  const { data, error, response } = await api.POST('/api/v1/projects/{projectId}/findings/{findingId}/actions', {
    params: { path: { projectId, findingId } }, body,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function listFindingActions(projectId: string, findingId: string, signal?: AbortSignal): Promise<FindingAction[]> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/findings/{findingId}/actions', {
    params: { path: { projectId, findingId } }, signal,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function createAnalysis(projectId: string, requestId: string): Promise<AnalysisRun> {
  const { data, error, response } = await api.POST('/api/v1/projects/{projectId}/analyses', {
    params: { path: { projectId } }, body: { requestId },
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function getAnalysis(projectId: string, analysisId: string, signal?: AbortSignal): Promise<AnalysisRun> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/analyses/{analysisId}', {
    params: { path: { projectId, analysisId } }, signal,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

export async function listFindings(projectId: string, analysisId?: string, page = 0, signal?: AbortSignal): Promise<Finding[]> {
  const { data, error, response } = await api.GET('/api/v1/projects/{projectId}/findings', {
    params: { path: { projectId }, query: { analysisId, page } }, signal,
  });
  if (!data) throw new ApiError(response.status, error);
  return data;
}

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
  public readonly analysisRunId?: string;
  constructor(public readonly status: number, problem?: Problem) {
    super(problem?.detail ?? 'SceneProof could not complete this request. Please try again.');
    this.analysisRunId = problem?.analysisRunId;
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
