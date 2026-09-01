import axios from 'axios'
import { getApiErrorMessage } from './deviceApi.js'

const api = axios.create({ baseURL: '/api', timeout: 30000 })

// 키스토어 검증처럼 백엔드가 구체적인 실패 사유를 내려주는 경우 그 메시지를 그대로 보여준다.
export function getServerErrorMessage(error) {
  return error.response?.data?.message || getApiErrorMessage(error)
}

export const projectApi = {
  getAll: () => api.get('/projects').then((response) => response.data),
  getById: (id) => api.get('/projects/' + id).then((response) => response.data),
  create: (payload) => api.post('/projects', payload).then((response) => response.data),
  update: (id, payload) => api.put('/projects/' + id, payload).then((response) => response.data),
  remove: (id) => api.delete('/projects/' + id),
  getDevices: (id) => api.get('/projects/' + id + '/devices').then((response) => response.data),
  getNetworks: (id) => api.get('/projects/' + id + '/networks').then((response) => response.data),
  createNetwork: (id, payload) => api.post('/projects/' + id + '/networks', payload).then((response) => response.data),
  updateNetwork: (id, networkId, payload) => api.put('/projects/' + id + '/networks/' + networkId, payload).then((response) => response.data),
  removeNetwork: (id, networkId) => api.delete('/projects/' + id + '/networks/' + networkId),
  getApks: (id) => api.get('/projects/' + id + '/apks').then((response) => response.data),
  uploadApk: (id, formData) => api.post('/projects/' + id + '/apks', formData).then((response) => response.data),
  removeApk: (id, apkId) => api.delete('/projects/' + id + '/apks/' + apkId),
  downloadApk: async (id, apk) => {
    const response = await api.get('/projects/' + id + '/apks/' + apk.id + '/download', { responseType: 'blob' })
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = apk.fileName
    link.click()
    URL.revokeObjectURL(url)
  },
  getAssignments: (deviceId) => api.get('/devices/' + deviceId + '/project-assignments').then((response) => response.data),
  getCurrentAssignment: (deviceId) => api.get('/devices/' + deviceId + '/project-assignments/current').then((response) => response.data),
  assignDevice: (deviceId, payload) => api.post('/devices/' + deviceId + '/project-assignments', payload).then((response) => response.data),
  endAssignment: (deviceId) => api.post('/devices/' + deviceId + '/project-assignments/end').then((response) => response.data),
}
