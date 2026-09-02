import axios from 'axios'

// 토큰은 sessionStorage에만 둔다.
// 메모리에만 두면 새로고침할 때마다 로그아웃되고, localStorage에 두면 탭을 닫아도 남는다.
// sessionStorage는 탭을 닫으면 사라지므로 학습용 관리자 웹에서 다루기 가장 단순한 절충이다.
const TOKEN_KEY = 'devicehub.accessToken'
const USER_KEY = 'devicehub.user'

const api = axios.create({ baseURL: '/api', timeout: 8000 })

const listeners = new Set()

export function getToken() {
  try {
    return sessionStorage.getItem(TOKEN_KEY)
  } catch (error) {
    return null
  }
}

export function getCurrentUser() {
  try {
    const raw = sessionStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (error) {
    return null
  }
}

function store(token, user) {
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token)
    if (user) sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  } catch (error) {
    // 스토리지를 쓸 수 없는 환경에서도 화면은 동작해야 한다.
  }
}

export function clearAuth() {
  try {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(USER_KEY)
  } catch (error) {
    // 무시한다.
  }
  listeners.forEach((listener) => listener())
}

/** 401을 받았을 때 앱이 로그인 화면으로 돌아가도록 알린다. */
export function onAuthCleared(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function hasKeystoreAccess(user) {
  return user?.role === 'ROLE_ADMIN' || user?.role === 'ROLE_RELEASE_MANAGER'
}

export function isAdmin(user) {
  return user?.role === 'ROLE_ADMIN'
}

/** 공통 axios 인스턴스에 Authorization 헤더와 401 처리를 붙인다. */
export function attachAuthInterceptors(instance) {
  instance.interceptors.request.use((config) => {
    const token = getToken()
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  })
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      // 401은 인증이 끊긴 것이므로 로그인 화면으로 돌려보낸다.
      // 403은 로그인은 되어 있고 권한만 없는 것이라 화면을 유지하고 메시지만 보여준다.
      if (error.response?.status === 401) clearAuth()
      return Promise.reject(error)
    },
  )
  return instance
}

attachAuthInterceptors(api)

export const authApi = {
  login: (payload) =>
    api.post('/auth/login', payload).then((response) => {
      store(response.data.accessToken, response.data.user)
      return response.data.user
    }),
  logout: () => clearAuth(),
}

export function getLoginErrorMessage(error) {
  if (error.code === 'ECONNABORTED') return '서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해 주세요.'
  if (!error.response) return '서버에 연결할 수 없습니다. 백엔드 실행 상태를 확인해 주세요.'
  if (error.response.status === 401) return '아이디 또는 비밀번호가 올바르지 않습니다.'
  if (error.response.status === 403) return '비활성화된 계정입니다. 관리자에게 문의해 주세요.'
  return '로그인을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function getForbiddenMessage(error) {
  if (error.response?.status === 403) return '이 기능을 사용할 권한이 없습니다.'
  return null
}
