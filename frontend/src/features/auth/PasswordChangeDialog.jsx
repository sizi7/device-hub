import { useState } from 'react'
import { userApi, getUserErrorMessage } from '../../api/userApi.js'
import styles from './PasswordChangeDialog.module.css'

export default function PasswordChangeDialog({ onClose }) {
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [error, setError] = useState('')
  const [done, setDone] = useState(false)
  const [loading, setLoading] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setError('')
    if (form.newPassword !== form.confirmPassword) {
      setError('새 비밀번호가 서로 다릅니다.')
      return
    }
    setLoading(true)
    try {
      await userApi.changeOwnPassword({ currentPassword: form.currentPassword, newPassword: form.newPassword })
      // 입력했던 비밀번호는 즉시 state에서 지운다.
      setForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setDone(true)
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
      setForm({ ...form, currentPassword: '' })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.modalLayer}>
      <div className={styles.backdrop} onClick={onClose} />
      <div className={styles.modal}>
        <div className={styles.modalHeader}><h2>비밀번호 변경</h2></div>
        {done ? (
          <>
            <p className={styles.success}>비밀번호를 변경했습니다. 이미 발급된 로그인 상태는 유지되며, 다음 로그인부터 새 비밀번호를 사용합니다.</p>
            <div className={styles.modalActions}>
              <button className={styles.primaryButton} type="button" onClick={onClose}>닫기</button>
            </div>
          </>
        ) : (
          <form onSubmit={submit}>
            <label className={styles.field}>
              <span>현재 비밀번호</span>
              <input required type="password" autoComplete="current-password" value={form.currentPassword} onChange={(event) => setForm({ ...form, currentPassword: event.target.value })} />
            </label>
            <label className={styles.field}>
              <span>새 비밀번호 (8자 이상)</span>
              <input required minLength={8} type="password" autoComplete="new-password" value={form.newPassword} onChange={(event) => setForm({ ...form, newPassword: event.target.value })} />
            </label>
            <label className={styles.field}>
              <span>새 비밀번호 확인</span>
              <input required minLength={8} type="password" autoComplete="new-password" value={form.confirmPassword} onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })} />
            </label>
            {error && <p className={styles.error} role="alert">{error}</p>}
            <div className={styles.modalActions}>
              <button className={styles.primaryButton} type="submit" disabled={loading}>{loading ? '변경 중...' : '변경'}</button>
              <button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
