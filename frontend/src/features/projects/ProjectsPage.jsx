import { useCallback, useEffect, useState } from 'react'
import { projectApi, getServerErrorMessage } from '../../api/projectApi.js'
import { getApiErrorMessage } from '../../api/deviceApi.js'
import styles from './ProjectsPage.module.css'

const emptyProject = { name: '', code: '', description: '', manager: '', status: 'PLANNING' }
const emptyNetwork = { environmentType: 'ISO', name: '', apiUrl: '', socketUrl: '', description: '' }
const emptyKeystore = { file: null, name: '', keyAlias: '', storePassword: '', keyPassword: '', description: '' }

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium' }).format(new Date(value))
}

const statusLabel = {
  PLANNING: '기획',
  DEVELOPMENT: '개발',
  OPERATING: '운영',
  SUSPENDED: '중단',
  COMPLETED: '완료',
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [form, setForm] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  const loadProjects = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try { setProjects(await projectApi.getAll()) } catch (requestError) { setError(getApiErrorMessage(requestError)) } finally { setIsLoading(false) }
  }, [])

  useEffect(() => { loadProjects() }, [loadProjects])

  const saveProject = async (event) => {
    event.preventDefault()
    try {
      if (form.id) await projectApi.update(form.id, form)
      else await projectApi.create(form)
      setForm(null)
      await loadProjects()
    } catch (requestError) {
      setError(requestError.response?.status === 409 ? '이미 사용 중인 프로젝트 코드입니다.' : getApiErrorMessage(requestError))
    }
  }

  if (selectedId) return <ProjectDetail projectId={selectedId} onBack={() => { setSelectedId(null); loadProjects() }} />

  return (
    <section>
      <div className={styles.pageHeading}>
        <div><p className={styles.eyebrow}>Project management</p><h1>Projects</h1><p>프로젝트, 연결 기기, 네트워크와 APK 배포 정보를 관리합니다.</p></div>
        <button className={styles.primaryButton} type="button" onClick={() => setForm({ ...emptyProject })}>프로젝트 등록</button>
      </div>
      {error && <div className={styles.error}>{error}</div>}
      <div className={styles.panel}>
        {isLoading ? <div className={styles.state}>프로젝트를 불러오는 중입니다.</div> : projects.length === 0 ? <div className={styles.state}>등록된 프로젝트가 없습니다.</div> : (
          <div className={styles.tableScroll}><table className={styles.table}>
            <thead><tr><th>프로젝트명</th><th>코드</th><th>상태</th><th>관리자</th><th>연결 기기</th><th>최신 APK</th><th>수정일</th><th>작업</th></tr></thead>
            <tbody>{projects.map((project) => <tr key={project.id}>
              <td><button className={styles.nameButton} type="button" onClick={() => setSelectedId(project.id)}>{project.name}</button></td>
              <td>{project.code}</td><td><span className={styles.badge}>{statusLabel[project.status]}</span></td>
              <td>{project.manager || '—'}</td><td>{project.connectedDeviceCount}대</td>
              <td>{project.latestApk ? project.latestApk.version + ' · ' + project.latestApk.environmentType : '—'}</td>
              <td>{formatDate(project.updatedAt)}</td>
              <td><button className={styles.textButton} type="button" onClick={() => setForm({ ...project })}>수정</button></td>
            </tr>)}</tbody>
          </table></div>
        )}
      </div>
      {form && <ProjectForm form={form} setForm={setForm} onSubmit={saveProject} onClose={() => setForm(null)} />}
    </section>
  )
}

function ProjectForm({ form, setForm, onSubmit, onClose }) {
  return <div className={styles.modalLayer}><div className={styles.backdrop} /><form className={styles.modal} onSubmit={onSubmit}>
    <div className={styles.modalHeader}><h2>{form.id ? '프로젝트 수정' : '프로젝트 등록'}</h2></div>
    <div className={styles.formBody}>
      <label>프로젝트명 *<input required value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
      <label>코드 *<input required pattern="[A-Z0-9_]+" value={form.code} onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase() })} placeholder="THYNC_PHYSICIAN" /></label>
      <label>상태<select value={form.status} onChange={(event) => setForm({ ...form, status: event.target.value })}>{Object.entries(statusLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
      <label>관리자<input value={form.manager || ''} onChange={(event) => setForm({ ...form, manager: event.target.value })} /></label>
      <label>설명<textarea rows="4" value={form.description || ''} onChange={(event) => setForm({ ...form, description: event.target.value })} /></label>
    </div>
    <div className={styles.modalActions}><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="submit">저장</button></div>
  </form></div>
}

function ProjectDetail({ projectId, onBack }) {
  const [project, setProject] = useState(null)
  const [devices, setDevices] = useState([])
  const [networks, setNetworks] = useState([])
  const [apks, setApks] = useState([])
  const [keystores, setKeystores] = useState([])
  const [tab, setTab] = useState('overview')
  const [networkForm, setNetworkForm] = useState(null)
  const [apkForm, setApkForm] = useState({ file: null, version: '', versionCode: '', environmentType: 'ISO', releaseNote: '' })
  const [keystoreForm, setKeystoreForm] = useState({ ...emptyKeystore })
  const [passwordForm, setPasswordForm] = useState(null)
  const [revealed, setRevealed] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setError('')
    try {
      const [projectResult, deviceResult, networkResult, apkResult, keystoreResult] = await Promise.all([projectApi.getById(projectId), projectApi.getDevices(projectId), projectApi.getNetworks(projectId), projectApi.getApks(projectId), projectApi.getKeystores(projectId)])
      setProject(projectResult); setDevices(deviceResult); setNetworks(networkResult); setApks(apkResult); setKeystores(keystoreResult)
    } catch (requestError) { setError(getApiErrorMessage(requestError)) }
  }, [projectId])
  useEffect(() => { load() }, [load])

  const saveNetwork = async (event) => {
    event.preventDefault()
    if (networkForm.id) await projectApi.updateNetwork(projectId, networkForm.id, networkForm)
    else await projectApi.createNetwork(projectId, networkForm)
    setNetworkForm(null); await load()
  }
  const uploadApk = async (event) => {
    event.preventDefault()
    const data = new FormData()
    data.append('file', apkForm.file); data.append('version', apkForm.version); data.append('versionCode', apkForm.versionCode); data.append('environmentType', apkForm.environmentType); data.append('releaseNote', apkForm.releaseNote)
    try { await projectApi.uploadApk(projectId, data); setApkForm({ file: null, version: '', versionCode: '', environmentType: 'ISO', releaseNote: '' }); event.target.reset(); await load() } catch (requestError) { setError(getApiErrorMessage(requestError)) }
  }
  const uploadKeystore = async (event) => {
    event.preventDefault()
    setError('')
    const data = new FormData()
    data.append('file', keystoreForm.file); data.append('name', keystoreForm.name); data.append('keyAlias', keystoreForm.keyAlias); data.append('storePassword', keystoreForm.storePassword)
    if (keystoreForm.keyPassword) data.append('keyPassword', keystoreForm.keyPassword)
    if (keystoreForm.description) data.append('description', keystoreForm.description)
    try { await projectApi.uploadKeystore(projectId, data); setKeystoreForm({ ...emptyKeystore }); event.target.reset(); await load() } catch (requestError) { setError(getServerErrorMessage(requestError)) }
  }
  const savePassword = async (event) => {
    event.preventDefault()
    setError('')
    try { await projectApi.updateKeystorePassword(projectId, passwordForm.id, passwordForm); setPasswordForm(null); await load() } catch (requestError) { setError(getServerErrorMessage(requestError)) }
  }
  const revealPassword = async (keystore) => {
    setError('')
    try { setRevealed({ ...await projectApi.revealKeystorePassword(projectId, keystore.id), name: keystore.name }) } catch (requestError) { setError(getServerErrorMessage(requestError)) }
  }
  const deleteKeystore = async (keystore) => {
    // 서명 키스토어를 잃으면 같은 앱을 다시 업데이트할 수 없으므로 삭제 전에 한 번 확인한다.
    if (!window.confirm(keystore.name + ' 키스토어를 삭제하면 같은 서명으로 앱을 업데이트할 수 없습니다. 삭제할까요?')) return
    setError('')
    try { await projectApi.removeKeystore(projectId, keystore.id); await load() } catch (requestError) { setError(getServerErrorMessage(requestError)) }
  }
  if (!project) return <div className={styles.state}>{error || '프로젝트를 불러오는 중입니다.'}</div>

  return <section>
    <button className={styles.backButton} type="button" onClick={onBack}>← 프로젝트 목록</button>
    <div className={styles.detailHeading}><div><p>{project.code}</p><h1>{project.name}</h1><span>{statusLabel[project.status]}</span></div></div>
    {error && <div className={styles.error}>{error}</div>}
    <div className={styles.tabs}>{[['overview','개요'],['devices','기기'],['networks','네트워크'],['apks','APK'],['keystores','키스토어']].map(([value,label]) => <button key={value} className={tab === value ? styles.activeTab : ''} type="button" onClick={() => setTab(value)}>{label}</button>)}</div>
    <div className={styles.detailPanel}>
      {tab === 'overview' && <dl className={styles.overview}><div><dt>프로젝트명</dt><dd>{project.name}</dd></div><div><dt>코드</dt><dd>{project.code}</dd></div><div><dt>상태</dt><dd>{statusLabel[project.status]}</dd></div><div><dt>관리자</dt><dd>{project.manager || '—'}</dd></div><div className={styles.full}><dt>설명</dt><dd>{project.description || '—'}</dd></div></dl>}
      {tab === 'devices' && <DataTable headers={['기기명','모델','현재 위치','설치 버전','최신 버전','상태']} rows={devices.map((device) => [device.name, device.modelName, device.currentLocation, device.installedVersion || '—', device.latestVersion || '—', device.versionStatus])} empty="연결된 기기가 없습니다." />}
      {tab === 'networks' && <><div className={styles.sectionAction}><h2>네트워크 환경</h2></div><DataTable headers={['환경','이름','API URL','Socket URL','작업']} rows={networks.map((network) => [network.environmentType, network.name, network.apiUrl || '—', network.socketUrl || '—', <span className={styles.rowActions}><button onClick={() => setNetworkForm({ ...network })}>수정</button><button onClick={async () => { await projectApi.removeNetwork(projectId, network.id); load() }}>삭제</button></span>])} empty="등록된 네트워크가 없습니다." />{networkForm && <NetworkForm form={networkForm} setForm={setNetworkForm} onSubmit={saveNetwork} />}</>}
      {tab === 'apks' && <><div className={styles.sectionAction}><h2>APK</h2></div><form className={styles.uploadForm} onSubmit={uploadApk}><input required type="file" accept=".apk" onChange={(event) => setApkForm({ ...apkForm, file: event.target.files[0] })} /><input required placeholder="버전 (1.5.2)" value={apkForm.version} onChange={(event) => setApkForm({ ...apkForm, version: event.target.value })} /><input required min="1" type="number" placeholder="versionCode" value={apkForm.versionCode} onChange={(event) => setApkForm({ ...apkForm, versionCode: event.target.value })} /><select value={apkForm.environmentType} onChange={(event) => setApkForm({ ...apkForm, environmentType: event.target.value })}>{['ISO','MFDS','DEVELOPMENT','BUSINESS'].map((value) => <option key={value}>{value}</option>)}</select><input placeholder="릴리즈 노트" value={apkForm.releaseNote} onChange={(event) => setApkForm({ ...apkForm, releaseNote: event.target.value })} /><button className={styles.primaryButton}>APK 업로드</button></form><DataTable headers={['버전','versionCode','환경','업로드일','파일명','작업']} rows={apks.map((apk) => [apk.version, apk.versionCode, apk.environmentType, formatDate(apk.uploadedAt), apk.fileName, <span className={styles.rowActions}><button onClick={() => projectApi.downloadApk(projectId, apk)}>다운로드</button><button onClick={async () => { await projectApi.removeApk(projectId, apk.id); load() }}>삭제</button></span>])} empty="업로드된 APK가 없습니다." /></>}
      {tab === 'keystores' && <>
        <div className={styles.sectionAction}><h2>서명 키스토어</h2></div>
        <p className={styles.notice}>비밀번호는 서버에서 암호화해 저장합니다. 등록할 때 실제 키스토어를 열어 alias와 비밀번호를 검증하므로, 값이 틀리면 저장되지 않습니다.</p>
        <form className={styles.keystoreForm} onSubmit={uploadKeystore}>
          <input required type="file" accept=".jks,.keystore,.p12,.pfx" onChange={(event) => setKeystoreForm({ ...keystoreForm, file: event.target.files[0] })} />
          <input required placeholder="이름 (Release Keystore)" value={keystoreForm.name} onChange={(event) => setKeystoreForm({ ...keystoreForm, name: event.target.value })} />
          <input required placeholder="키 alias" value={keystoreForm.keyAlias} onChange={(event) => setKeystoreForm({ ...keystoreForm, keyAlias: event.target.value })} />
          <input required type="password" autoComplete="new-password" placeholder="스토어 비밀번호" value={keystoreForm.storePassword} onChange={(event) => setKeystoreForm({ ...keystoreForm, storePassword: event.target.value })} />
          <input type="password" autoComplete="new-password" placeholder="키 비밀번호 (비우면 스토어와 동일)" value={keystoreForm.keyPassword} onChange={(event) => setKeystoreForm({ ...keystoreForm, keyPassword: event.target.value })} />
          <input placeholder="설명" value={keystoreForm.description} onChange={(event) => setKeystoreForm({ ...keystoreForm, description: event.target.value })} />
          <button className={styles.primaryButton}>키스토어 등록</button>
        </form>
        <DataTable headers={['이름','파일명','형식','alias','키 비밀번호','등록일','작업']} rows={keystores.map((keystore) => [keystore.name, keystore.fileName, keystore.storeType, keystore.keyAlias, keystore.hasSeparateKeyPassword ? '별도 지정' : '스토어와 동일', formatDate(keystore.createdAt), <span className={styles.rowActions}><button onClick={() => revealPassword(keystore)}>비밀번호 보기</button><button onClick={() => setPasswordForm({ id: keystore.id, keyAlias: keystore.keyAlias, storePassword: '', keyPassword: '' })}>비밀번호 변경</button><button onClick={() => projectApi.downloadKeystore(projectId, keystore)}>다운로드</button><button onClick={() => deleteKeystore(keystore)}>삭제</button></span>])} empty="등록된 키스토어가 없습니다." />
        {passwordForm && <KeystorePasswordForm form={passwordForm} setForm={setPasswordForm} onSubmit={savePassword} />}
        {revealed && <KeystoreRevealDialog revealed={revealed} onClose={() => setRevealed(null)} />}
      </>}
    </div>
  </section>
}

function KeystorePasswordForm({ form, setForm, onSubmit }) {
  return <form className={styles.inlineForm} onSubmit={onSubmit}>
    <input required placeholder="키 alias" value={form.keyAlias} onChange={(event) => setForm({ ...form, keyAlias: event.target.value })} />
    <input required type="password" autoComplete="new-password" placeholder="새 스토어 비밀번호" value={form.storePassword} onChange={(event) => setForm({ ...form, storePassword: event.target.value })} />
    <input type="password" autoComplete="new-password" placeholder="새 키 비밀번호 (비우면 스토어와 동일)" value={form.keyPassword} onChange={(event) => setForm({ ...form, keyPassword: event.target.value })} />
    <button className={styles.primaryButton}>저장</button>
    <button className={styles.secondaryButton} type="button" onClick={() => setForm(null)}>취소</button>
  </form>
}

function KeystoreRevealDialog({ revealed, onClose }) {
  return <div className={styles.modalLayer}><div className={styles.backdrop} onClick={onClose} /><div className={styles.modal}>
    <div className={styles.modalHeader}><h2>{revealed.name} 비밀번호</h2></div>
    <dl className={styles.secretList}>
      <div><dt>키 alias</dt><dd>{revealed.keyAlias}</dd></div>
      <div><dt>스토어 비밀번호</dt><dd>{revealed.storePassword}</dd></div>
      <div><dt>키 비밀번호</dt><dd>{revealed.keyPassword}</dd></div>
    </dl>
    <div className={styles.modalActions}><button className={styles.primaryButton} type="button" onClick={onClose}>닫기</button></div>
  </div></div>
}

function DataTable({ headers, rows, empty }) {
  if (rows.length === 0) return <div className={styles.state}>{empty}</div>
  return <div className={styles.tableScroll}><table className={styles.table}><thead><tr>{headers.map((header) => <th key={header}>{header}</th>)}</tr></thead><tbody>{rows.map((row, index) => <tr key={index}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}</tr>)}</tbody></table></div>
}

function NetworkForm({ form, setForm, onSubmit }) {
  return <form className={styles.inlineForm} onSubmit={onSubmit}><select value={form.environmentType} onChange={(event) => setForm({ ...form, environmentType: event.target.value })}>{['ISO','MFDS','DEVELOPMENT','BUSINESS'].map((value) => <option key={value}>{value}</option>)}</select><input required placeholder="설정 이름" value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /><input placeholder="API URL" value={form.apiUrl || ''} onChange={(event) => setForm({ ...form, apiUrl: event.target.value })} /><input placeholder="Socket URL" value={form.socketUrl || ''} onChange={(event) => setForm({ ...form, socketUrl: event.target.value })} /><input placeholder="설명" value={form.description || ''} onChange={(event) => setForm({ ...form, description: event.target.value })} /><button className={styles.primaryButton}>저장</button><button className={styles.secondaryButton} type="button" onClick={() => setForm(null)}>취소</button></form>
}
