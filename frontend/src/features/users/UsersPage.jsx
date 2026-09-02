import { useCallback, useEffect, useState } from 'react'
import { userApi, getUserErrorMessage } from '../../api/userApi.js'
import styles from './UsersPage.module.css'

const roleLabels = {
  ROLE_ADMIN: '관리자',
  ROLE_RELEASE_MANAGER: '배포 담당자',
  ROLE_USER: '일반 사용자',
}

const roleDescriptions = {
  ROLE_ADMIN: '전체 관리와 사용자 관리, 키스토어 모든 작업',
  ROLE_RELEASE_MANAGER: 'APK와 키스토어 등 배포 관련 관리',
  ROLE_USER: '기기와 프로젝트 조회. 키스토어 비밀번호와 파일에는 접근 불가',
}

const emptyCreateForm = { username: '', password: '', name: '', role: 'ROLE_USER' }

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export default function UsersPage({ user }) {
  const [users, setUsers] = useState([])
  const [createForm, setCreateForm] = useState(null)
  const [editForm, setEditForm] = useState(null)
  const [passwordForm, setPasswordForm] = useState(null)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setError('')
    setLoading(true)
    try {
      setUsers(await userApi.getAll())
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const submitCreate = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await userApi.create({ ...createForm, username: createForm.username.trim(), name: createForm.name.trim() })
      // 입력했던 비밀번호는 화면 state에 남기지 않는다.
      setCreateForm(null)
      setNotice('사용자를 추가했습니다.')
      await load()
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
      setCreateForm({ ...createForm, password: '' })
    }
  }

  const submitEdit = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await userApi.update(editForm.id, { name: editForm.name.trim(), role: editForm.role, enabled: editForm.enabled })
      setEditForm(null)
      setNotice('사용자 정보를 수정했습니다.')
      await load()
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
    }
  }

  const submitPassword = async (event) => {
    event.preventDefault()
    setError('')
    try {
      await userApi.resetPassword(passwordForm.id, { newPassword: passwordForm.newPassword })
      setPasswordForm(null)
      setNotice('비밀번호를 재설정했습니다. 새 비밀번호를 본인에게 직접 전달해 주세요.')
      await load()
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
      setPasswordForm({ ...passwordForm, newPassword: '' })
    }
  }

  const removeUser = async (target) => {
    if (!window.confirm(target.username + ' 계정을 삭제할까요? 되돌릴 수 없습니다.')) return
    setError('')
    try {
      await userApi.remove(target.id)
      setNotice('사용자를 삭제했습니다.')
      await load()
    } catch (requestError) {
      setError(getUserErrorMessage(requestError))
    }
  }

  return (
    <section>
      <div className={styles.pageHeading}>
        <div>
          <p className={styles.eyebrow}>Users</p>
          <h1>사용자 관리</h1>
          <p>로그인 계정과 역할을 관리합니다. 비밀번호는 해시로만 저장되어 확인할 수 없고 재설정만 가능합니다.</p>
        </div>
        <button className={styles.primaryButton} type="button" onClick={() => { setCreateForm({ ...emptyCreateForm }); setNotice('') }}>
          사용자 추가
        </button>
      </div>

      {error && <p className={styles.error} role="alert">{error}</p>}
      {notice && <p className={styles.notice}>{notice}</p>}

      {createForm && (
        <form className={styles.panelForm} onSubmit={submitCreate}>
          <h2>사용자 추가</h2>
          <div className={styles.formRow}>
            <label>
              <span>아이디</span>
              <input required minLength={3} autoComplete="off" value={createForm.username} onChange={(event) => setCreateForm({ ...createForm, username: event.target.value })} />
            </label>
            <label>
              <span>표시 이름</span>
              <input required value={createForm.name} onChange={(event) => setCreateForm({ ...createForm, name: event.target.value })} />
            </label>
            <label>
              <span>비밀번호 (8자 이상)</span>
              <input required minLength={8} type="password" autoComplete="new-password" value={createForm.password} onChange={(event) => setCreateForm({ ...createForm, password: event.target.value })} />
            </label>
            <label>
              <span>역할</span>
              <select value={createForm.role} onChange={(event) => setCreateForm({ ...createForm, role: event.target.value })}>
                {Object.keys(roleLabels).map((value) => <option key={value} value={value}>{roleLabels[value]}</option>)}
              </select>
            </label>
          </div>
          <p className={styles.roleHint}>{roleDescriptions[createForm.role]}</p>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="submit">추가</button>
            <button className={styles.secondaryButton} type="button" onClick={() => setCreateForm(null)}>취소</button>
          </div>
        </form>
      )}

      {editForm && (
        <form className={styles.panelForm} onSubmit={submitEdit}>
          <h2>{editForm.username} 수정</h2>
          <div className={styles.formRow}>
            <label>
              <span>표시 이름</span>
              <input required value={editForm.name} onChange={(event) => setEditForm({ ...editForm, name: event.target.value })} />
            </label>
            <label>
              <span>역할</span>
              <select value={editForm.role} onChange={(event) => setEditForm({ ...editForm, role: event.target.value })}>
                {Object.keys(roleLabels).map((value) => <option key={value} value={value}>{roleLabels[value]}</option>)}
              </select>
            </label>
            <label className={styles.checkboxField}>
              <input type="checkbox" checked={editForm.enabled} onChange={(event) => setEditForm({ ...editForm, enabled: event.target.checked })} />
              <span>계정 활성</span>
            </label>
          </div>
          <p className={styles.roleHint}>{roleDescriptions[editForm.role]}</p>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="submit">저장</button>
            <button className={styles.secondaryButton} type="button" onClick={() => setEditForm(null)}>취소</button>
          </div>
        </form>
      )}

      {passwordForm && (
        <form className={styles.panelForm} onSubmit={submitPassword}>
          <h2>{passwordForm.username} 비밀번호 재설정</h2>
          <p className={styles.roleHint}>기존 비밀번호는 확인할 수 없습니다. 새 비밀번호를 설정한 뒤 본인에게 직접 전달해 주세요.</p>
          <div className={styles.formRow}>
            <label>
              <span>새 비밀번호 (8자 이상)</span>
              <input required minLength={8} type="password" autoComplete="new-password" value={passwordForm.newPassword} onChange={(event) => setPasswordForm({ ...passwordForm, newPassword: event.target.value })} />
            </label>
          </div>
          <div className={styles.formActions}>
            <button className={styles.primaryButton} type="submit">재설정</button>
            <button className={styles.secondaryButton} type="button" onClick={() => setPasswordForm(null)}>취소</button>
          </div>
        </form>
      )}

      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>아이디</th>
              <th>이름</th>
              <th>역할</th>
              <th>상태</th>
              <th>생성일</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {users.map((row) => (
              <tr key={row.id}>
                <td>
                  {row.username}
                  {row.id === user?.id && <span className={styles.selfBadge}>나</span>}
                </td>
                <td>{row.name}</td>
                <td>{roleLabels[row.role] || row.role}</td>
                <td>
                  <span className={row.enabled ? styles.enabled : styles.disabled}>
                    {row.enabled ? '활성' : '비활성'}
                  </span>
                </td>
                <td>{formatDate(row.createdAt)}</td>
                <td>
                  <span className={styles.rowActions}>
                    <button type="button" onClick={() => { setEditForm({ id: row.id, username: row.username, name: row.name, role: row.role, enabled: row.enabled }); setNotice('') }}>수정</button>
                    <button type="button" onClick={() => { setPasswordForm({ id: row.id, username: row.username, newPassword: '' }); setNotice('') }}>비밀번호 재설정</button>
                    <button type="button" disabled={row.id === user?.id} onClick={() => removeUser(row)}>삭제</button>
                  </span>
                </td>
              </tr>
            ))}
            {!loading && users.length === 0 && (
              <tr><td colSpan={6} className={styles.empty}>등록된 사용자가 없습니다.</td></tr>
            )}
            {loading && <tr><td colSpan={6} className={styles.empty}>불러오는 중...</td></tr>}
          </tbody>
        </table>
      </div>
    </section>
  )
}
