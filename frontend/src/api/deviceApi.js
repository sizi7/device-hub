import axios from 'axios'
import { attachAuthInterceptors } from './authApi.js'

const api = axios.create({
  baseURL: '/api',
  timeout: 8000,
})

// 모든 요청에 Authorization 헤더를 붙이고 401이면 인증 상태를 초기화한다.
attachAuthInterceptors(api)

export const deviceApi = {
  getAll: () => api.get('/devices').then((response) => response.data),
  getConnected: () => api.get('/devices/connected').then((response) => response.data),
  getById: (id) => api.get(`/devices/${id}`).then((response) => response.data),
  create: (payload) => api.post('/devices', payload).then((response) => response.data),
  update: (id, payload) => api.put(`/devices/${id}`, payload).then((response) => response.data),
  remove: (id) => api.delete(`/devices/${id}`),
  getDeployments: (deviceId) => api.get(`/devices/${deviceId}/deployments`).then((response) => response.data),
  getCurrentDeployment: (deviceId) => api.get(`/devices/${deviceId}/deployments/current`).then((response) => response.data),
  deploy: (deviceId, payload) => api.post(`/devices/${deviceId}/deployments`, payload).then((response) => response.data),
  returnDeployment: (deviceId) => api.post(`/devices/${deviceId}/deployments/return`).then((response) => response.data),
  getProjects: (deviceId) => api.get(`/devices/${deviceId}/projects`).then((response) => response.data),
  createProject: (deviceId, payload) => api.post(`/devices/${deviceId}/projects`, payload).then((response) => response.data),
  updateProject: (deviceId, projectId, payload) => api.put(`/devices/${deviceId}/projects/${projectId}`, payload).then((response) => response.data),
  removeProject: (deviceId, projectId) => api.delete(`/devices/${deviceId}/projects/${projectId}`),
}

export function getApiErrorMessage(error) {
  if (error.code === 'ECONNABORTED') return '서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
  if (!error.response) return '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
  if (error.response.status === 401) return '로그인이 필요합니다.'
  if (error.response.status === 403) return '이 기능을 사용할 권한이 없습니다.'
  if (error.response.status === 400) return '입력값을 확인해 주세요.'
  if (error.response.status === 404) return '요청한 기기 정보를 찾을 수 없습니다.'
  if (error.response.status === 409) return '이미 등록된 기기입니다.'
  return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}
