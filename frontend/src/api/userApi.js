import axios from 'axios'
import { attachAuthInterceptors } from './authApi.js'

const api = axios.create({ baseURL: '/api', timeout: 8000 })

attachAuthInterceptors(api)

export const userApi = {
  getAll: () => api.get('/users').then((response) => response.data),
  create: (payload) => api.post('/users', payload).then((response) => response.data),
  update: (id, payload) => api.put('/users/' + id, payload).then((response) => response.data),
  resetPassword: (id, payload) => api.put('/users/' + id + '/password', payload).then((response) => response.data),
  remove: (id) => api.delete('/users/' + id),
  // 본인 비밀번호 변경은 역할과 무관하게 누구나 호출할 수 있어 /api/auth 아래에 있다.
  changeOwnPassword: (payload) => api.put('/auth/password', payload).then((response) => response.data),
}

export function getUserErrorMessage(error) {
  if (error.code === 'ECONNABORTED') return '서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
  if (!error.response) return '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
  // 백엔드가 구체적인 사유를 내려주면 그대로 보여준다.
  if (error.response.data?.message) return error.response.data.message
  if (error.response.status === 400) return '입력값을 확인해 주세요.'
  if (error.response.status === 403) return '이 기능을 사용할 권한이 없습니다.'
  if (error.response.status === 404) return '사용자를 찾을 수 없습니다.'
  if (error.response.status === 409) return '요청을 처리할 수 없습니다.'
  return '요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}
