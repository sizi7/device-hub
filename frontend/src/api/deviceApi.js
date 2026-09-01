import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 8000,
})

export const deviceApi = {
  getAll: () => api.get('/devices').then((response) => response.data),
  getById: (id) => api.get(`/devices/${id}`).then((response) => response.data),
  create: (payload) => api.post('/devices', payload).then((response) => response.data),
  update: (id, payload) => api.put(`/devices/${id}`, payload).then((response) => response.data),
  remove: (id) => api.delete(`/devices/${id}`),
}

export function getApiErrorMessage(error) {
  if (error.code === 'ECONNABORTED') return '서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
  if (!error.response) return '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
  if (error.response.status === 400) return '입력값을 확인해 주세요.'
  if (error.response.status === 404) return '요청한 기기 정보를 찾을 수 없습니다.'
  return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}
