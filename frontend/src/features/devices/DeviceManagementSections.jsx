import { useCallback, useEffect, useState } from 'react'
import { deviceApi, getApiErrorMessage } from '../../api/deviceApi.js'
import styles from './DeviceManagementSections.module.css'

const emptyProject = {
  projectName: '',
  packageName: '',
  installedVersion: '',
  latestVersion: '',
  lastUpdatedAt: '',
}

function localDateTimeValue() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16)
}

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value))
}

function deploymentLabel(type) {
  return type === 'HOSPITAL_LOAN' ? '병원 대여' : '병원 전용'
}

const statusText = {
  LATEST: '최신',
  UPDATE_REQUIRED: '업데이트 필요',
  UNKNOWN: '확인 필요',
}

export default function DeviceManagementSections({ deviceId }) {
  const [current, setCurrent] = useState(null)
  const [history, setHistory] = useState([])
  const [projects, setProjects] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [showHistory, setShowHistory] = useState(false)
  const [deploymentForm, setDeploymentForm] = useState(null)
  const [projectForm, setProjectForm] = useState(null)
  const [isSaving, setIsSaving] = useState(false)

  const loadManagementData = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const [currentResult, historyResult, projectResult] = await Promise.all([
        deviceApi.getCurrentDeployment(deviceId),
        deviceApi.getDeployments(deviceId),
        deviceApi.getProjects(deviceId),
      ])
      setCurrent(currentResult.deployment)
      setHistory(historyResult)
      setProjects(projectResult)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsLoading(false)
    }
  }, [deviceId])

  useEffect(() => {
    loadManagementData()
  }, [loadManagementData])

  const submitDeployment = async (event) => {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    try {
      await deviceApi.deploy(deviceId, {
        ...deploymentForm,
        deployedAt: deploymentForm.deployedAt + ':00',
        note: deploymentForm.note.trim() || null,
      })
      setDeploymentForm(null)
      await loadManagementData()
    } catch (requestError) {
      setError(requestError.response?.status === 409 ? '이미 병원에 배치 중인 기기입니다.' : getApiErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  const returnDeployment = async () => {
    setIsSaving(true)
    setError('')
    try {
      await deviceApi.returnDeployment(deviceId)
      await loadManagementData()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  const openProjectForm = (project = null) => {
    setProjectForm(project ? {
      id: project.id,
      projectName: project.projectName,
      packageName: project.packageName || '',
      installedVersion: project.installedVersion || '',
      latestVersion: project.latestVersion || '',
      lastUpdatedAt: project.lastUpdatedAt ? project.lastUpdatedAt.slice(0, 16) : '',
    } : { ...emptyProject })
  }

  const submitProject = async (event) => {
    event.preventDefault()
    setIsSaving(true)
    setError('')
    const payload = {
      projectName: projectForm.projectName.trim(),
      packageName: projectForm.packageName.trim() || null,
      installedVersion: projectForm.installedVersion.trim() || null,
      latestVersion: projectForm.latestVersion.trim() || null,
      lastUpdatedAt: projectForm.lastUpdatedAt ? projectForm.lastUpdatedAt + ':00' : null,
    }
    try {
      if (projectForm.id) {
        await deviceApi.updateProject(deviceId, projectForm.id, payload)
      } else {
        await deviceApi.createProject(deviceId, payload)
      }
      setProjectForm(null)
      await loadManagementData()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  const removeProject = async (projectId) => {
    setIsSaving(true)
    setError('')
    try {
      await deviceApi.removeProject(deviceId, projectId)
      await loadManagementData()
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) return <div className={styles.loading}>관리 정보를 불러오는 중입니다.</div>

  return (
    <div className={styles.sections}>
      {error && <div className={styles.error} role="alert">{error}</div>}

      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <div><h3>병원 배치 정보</h3><p>현재 위치와 과거 배치 이력을 관리합니다.</p></div>
          {current ? (
            <button className={styles.secondaryButton} type="button" disabled={isSaving} onClick={returnDeployment}>회수 처리</button>
          ) : (
            <button className={styles.primaryButton} type="button" onClick={() => setDeploymentForm({ hospitalName: '', deploymentType: 'HOSPITAL_LOAN', deployedAt: localDateTimeValue(), note: '' })}>병원 배치</button>
          )}
        </div>
        <dl className={styles.statusGrid}>
          <div><dt>현재 상태</dt><dd>{current ? deploymentLabel(current.deploymentType) : '사내'}</dd></div>
          <div><dt>병원</dt><dd>{current?.hospitalName || '—'}</dd></div>
          <div><dt>배치 유형</dt><dd>{current ? deploymentLabel(current.deploymentType) : '—'}</dd></div>
          <div><dt>배치일</dt><dd>{formatDate(current?.deployedAt)}</dd></div>
        </dl>
        <button className={styles.textButton} type="button" onClick={() => setShowHistory((value) => !value)}>
          {showHistory ? '배치 이력 닫기' : '배치 이력 보기'} ({history.length})
        </button>
        {showHistory && (
          <div className={styles.history}>
            {history.length === 0 ? <p>병원 배치 이력이 없습니다.</p> : history.map((item) => (
              <div key={item.id}>
                <strong>{item.hospitalName} · {deploymentLabel(item.deploymentType)}</strong>
                <span>{formatDate(item.deployedAt)} ~ {item.returnedAt ? formatDate(item.returnedAt) : '배치 중'}</span>
                {item.note && <p>{item.note}</p>}
              </div>
            ))}
          </div>
        )}
      </section>

      <section className={styles.section}>
        <div className={styles.sectionHeader}>
          <div><h3>프로젝트 / 앱</h3><p>기기에 설치된 프로젝트와 배포 버전을 관리합니다.</p></div>
          <button className={styles.primaryButton} type="button" onClick={() => openProjectForm()}>프로젝트 추가</button>
        </div>
        {projects.length === 0 ? (
          <div className={styles.empty}>등록된 프로젝트가 없습니다.</div>
        ) : (
          <div className={styles.projectScroll}>
            <table className={styles.projectTable}>
              <thead><tr><th>프로젝트명</th><th>현재</th><th>최신</th><th>업데이트</th><th>상태</th><th>작업</th></tr></thead>
              <tbody>{projects.map((project) => (
                <tr key={project.id}>
                  <td><strong>{project.projectName}</strong>{project.packageName && <span>{project.packageName}</span>}</td>
                  <td>{project.installedVersion || '—'}</td>
                  <td>{project.latestVersion || '—'}</td>
                  <td>{formatDate(project.lastUpdatedAt)}</td>
                  <td><span className={[styles.badge, styles[project.versionStatus]].join(' ')}>{statusText[project.versionStatus]}</span></td>
                  <td><div className={styles.rowActions}><button type="button" onClick={() => openProjectForm(project)}>수정</button><button type="button" disabled={isSaving} onClick={() => removeProject(project.id)}>삭제</button></div></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>

      {deploymentForm && (
        <div className={styles.modalLayer}>
          <div className={styles.modalBackdrop} />
          <form className={styles.modal} onSubmit={submitDeployment}>
            <div className={styles.modalHeader}><h3>병원 배치</h3><p>배치 유형과 병원 정보를 입력하세요.</p></div>
            <div className={styles.formBody}>
              <label>병원명 *<input required value={deploymentForm.hospitalName} onChange={(event) => setDeploymentForm({ ...deploymentForm, hospitalName: event.target.value })} /></label>
              <label>배치 유형 *<select value={deploymentForm.deploymentType} onChange={(event) => setDeploymentForm({ ...deploymentForm, deploymentType: event.target.value })}><option value="HOSPITAL_LOAN">병원 대여</option><option value="HOSPITAL_DEDICATED">병원 전용</option></select></label>
              <label>배치일 *<input required type="datetime-local" value={deploymentForm.deployedAt} onChange={(event) => setDeploymentForm({ ...deploymentForm, deployedAt: event.target.value })} /></label>
              <label>메모<textarea rows="3" value={deploymentForm.note} onChange={(event) => setDeploymentForm({ ...deploymentForm, note: event.target.value })} /></label>
            </div>
            <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={() => setDeploymentForm(null)}>취소</button><button className={styles.primaryButton} type="submit" disabled={isSaving}>{isSaving ? '저장 중...' : '배치'}</button></div>
          </form>
        </div>
      )}

      {projectForm && (
        <div className={styles.modalLayer}>
          <div className={styles.modalBackdrop} />
          <form className={styles.modal} onSubmit={submitProject}>
            <div className={styles.modalHeader}><h3>{projectForm.id ? '프로젝트 수정' : '프로젝트 추가'}</h3><p>기기별 설치 및 최신 버전을 관리합니다.</p></div>
            <div className={styles.formBody}>
              <label>프로젝트명 *<input required value={projectForm.projectName} onChange={(event) => setProjectForm({ ...projectForm, projectName: event.target.value })} /></label>
              <label>Package Name<input value={projectForm.packageName} onChange={(event) => setProjectForm({ ...projectForm, packageName: event.target.value })} placeholder="com.example.app" /></label>
              <div className={styles.formRow}><label>현재 버전<input value={projectForm.installedVersion} onChange={(event) => setProjectForm({ ...projectForm, installedVersion: event.target.value })} /></label><label>최신 버전<input value={projectForm.latestVersion} onChange={(event) => setProjectForm({ ...projectForm, latestVersion: event.target.value })} /></label></div>
              <label>마지막 업데이트<input type="datetime-local" value={projectForm.lastUpdatedAt} onChange={(event) => setProjectForm({ ...projectForm, lastUpdatedAt: event.target.value })} /></label>
            </div>
            <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={() => setProjectForm(null)}>취소</button><button className={styles.primaryButton} type="submit" disabled={isSaving}>{isSaving ? '저장 중...' : '저장'}</button></div>
          </form>
        </div>
      )}
    </div>
  )
}
