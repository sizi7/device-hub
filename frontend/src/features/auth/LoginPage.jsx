import { useState } from 'react'
import { authApi, getLoginErrorMessage } from '../../api/authApi.js'
import Icon from '../../components/common/Icon.jsx'
import styles from './LoginPage.module.css'

export default function LoginPage({ onLoggedIn }) {
  const [form, setForm] = useState({ username: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const user = await authApi.login({ username: form.username.trim(), password: form.password })
      // 입력했던 비밀번호는 화면 state에 남기지 않는다.
      setForm({ username: '', password: '' })
      onLoggedIn(user)
    } catch (loginError) {
      setError(getLoginErrorMessage(loginError))
      setForm({ ...form, password: '' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.page}>
      <form className={styles.card} onSubmit={submit}>
        <div className={styles.brand}>
          <span className={styles.brandMark}><Icon name="devices" size={21} /></span>
          <span>DeviceHub</span>
        </div>
        <h1 className={styles.title}>관리자 콘솔 로그인</h1>
        <p className={styles.subtitle}>계정은 관리자에게 요청해 주세요.</p>

        <label className={styles.field}>
          <span>아이디</span>
          <input
            required
            autoFocus
            autoComplete="username"
            value={form.username}
            onChange={(event) => setForm({ ...form, username: event.target.value })}
          />
        </label>

        <label className={styles.field}>
          <span>비밀번호</span>
          <input
            required
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
          />
        </label>

        {error && <p className={styles.error} role="alert">{error}</p>}

        <button className={styles.submit} type="submit" disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </form>
    </div>
  )
}
