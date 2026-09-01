import { useEffect, useState } from 'react'
import Icon from '../../components/common/Icon.jsx'
import { deviceApi, getApiErrorMessage } from '../../api/deviceApi.js'
import styles from './DeviceDrawer.module.css'
import DeviceManagementSections from './DeviceManagementSections.jsx'

const emptyForm = {
  name: '',
  type: 'PHONE',
  manufacturer: '',
  modelName: '',
  osVersion: '',
  serialNumber: '',
}

const modeText = {
  create: { eyebrow: 'New device', title: '기기 등록', description: '업무에 사용할 새 기기 정보를 입력하세요.' },
  edit: { eyebrow: 'Edit device', title: '기기 수정', description: '등록된 기기 정보를 최신 상태로 변경하세요.' },
  detail: { eyebrow: 'Device details', title: '기기 상세', description: '등록된 기기의 상세 정보입니다.' },
}

function formatDateTime(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}

export default function DeviceDrawer({ mode, device, onClose, onSaved }) {
  const [form, setForm] = useState(device ? {
    name: device.name,
    type: device.type,
    manufacturer: device.manufacturer,
    modelName: device.modelName,
    osVersion: device.osVersion || '',
    serialNumber: device.serialNumber || '',
  } : emptyForm)
  const [fieldErrors, setFieldErrors] = useState({})
  const [submitError, setSubmitError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const isDetail = mode === 'detail'
  const text = modeText[mode]

  useEffect(() => {
    const handleEscape = (event) => event.key === 'Escape' && onClose()
    document.addEventListener('keydown', handleEscape)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', handleEscape)
      document.body.style.overflow = ''
    }
  }, [onClose])

  const updateField = (event) => {
    const { name, value } = event.target
    setForm((current) => ({ ...current, [name]: value }))
    setFieldErrors((current) => ({ ...current, [name]: '' }))
  }

  const validate = () => {
    const errors = {}
    if (!form.name.trim()) errors.name = '기기 이름을 입력해 주세요.'
    if (!form.type) errors.type = '기기 타입을 선택해 주세요.'
    if (!form.manufacturer.trim()) errors.manufacturer = '제조사를 입력해 주세요.'
    if (!form.modelName.trim()) errors.modelName = '모델명을 입력해 주세요.'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    if (!validate()) return
    setIsSaving(true)
    setSubmitError('')
    const payload = {
      name: form.name.trim(),
      type: form.type,
      manufacturer: form.manufacturer.trim(),
      modelName: form.modelName.trim(),
      osVersion: form.osVersion.trim() || null,
      ...(mode === 'create' && form.serialNumber ? { serialNumber: form.serialNumber } : {}),
    }
    try {
      const saved = mode === 'create'
        ? await deviceApi.create(payload)
        : await deviceApi.update(device.id, payload)
      onSaved(saved)
    } catch (requestError) {
      setSubmitError(getApiErrorMessage(requestError))
    } finally {
      setIsSaving(false)
    }
  }

  return (
    <div className={styles.layer} role="presentation">
      <button className={styles.backdrop} type="button" onClick={onClose} aria-label="닫기" />
      <aside className={styles.drawer} role="dialog" aria-modal="true" aria-labelledby="drawer-title">
        <div className={styles.drawerHeader}>
          <div>
            <span>{text.eyebrow}</span>
            <h2 id="drawer-title">{text.title}</h2>
            <p>{text.description}</p>
          </div>
          <button className={styles.closeButton} type="button" onClick={onClose} aria-label="Drawer 닫기">
            <Icon name="close" size={21} />
          </button>
        </div>

        {isDetail ? (
          <div className={styles.detailBody}>
            <div className={styles.deviceIdentity}>
              <span className={styles.deviceGlyph}><Icon name="devices" size={25} /></span>
              <div>
                <h3>{device.name}</h3>
                <span className={styles.typeBadge}>{device.type === 'PHONE' ? '스마트폰' : '태블릿'}</span>
              </div>
            </div>
            <dl className={styles.detailList}>
              <div><dt>Device ID</dt><dd>#{device.id}</dd></div>
              <div><dt>제조사</dt><dd>{device.manufacturer}</dd></div>
              <div><dt>모델명</dt><dd>{device.modelName}</dd></div>
              <div><dt>OS 버전</dt><dd>{device.osVersion || '미입력'}</dd></div>
              <div><dt>Serial</dt><dd>{device.serialNumber || '수동 등록'}</dd></div>
              <div><dt>등록일</dt><dd>{formatDateTime(device.createdAt)}</dd></div>
              <div><dt>마지막 수정</dt><dd>{formatDateTime(device.updatedAt)}</dd></div>
            </dl>
            <DeviceManagementSections deviceId={device.id} />
          </div>
        ) : (
          <form className={styles.form} onSubmit={handleSubmit} noValidate>
            <div className={styles.formBody}>
              {submitError && <div className={styles.formError} role="alert"><Icon name="alert" size={18} />{submitError}</div>}
              <div className={styles.field}>
                <label htmlFor="device-name">기기 이름 <span>*</span></label>
                <input id="device-name" name="name" value={form.name} onChange={updateField} placeholder="예: 개발용 갤럭시" aria-invalid={Boolean(fieldErrors.name)} />
                {fieldErrors.name && <p>{fieldErrors.name}</p>}
              </div>
              <div className={styles.field}>
                <label htmlFor="device-type">기기 타입 <span>*</span></label>
                <select id="device-type" name="type" value={form.type} onChange={updateField}>
                  <option value="" disabled>기기 타입 선택</option>
                  <option value="PHONE">스마트폰</option>
                  <option value="TABLET">태블릿</option>
                </select>
                {fieldErrors.type && <p>{fieldErrors.type}</p>}
              </div>
              <div className={styles.field}>
                <label htmlFor="manufacturer">제조사 <span>*</span></label>
                <input id="manufacturer" name="manufacturer" value={form.manufacturer} onChange={updateField} placeholder="예: Samsung" aria-invalid={Boolean(fieldErrors.manufacturer)} />
                {fieldErrors.manufacturer && <p>{fieldErrors.manufacturer}</p>}
              </div>
              <div className={styles.field}>
                <label htmlFor="model-name">모델명 <span>*</span></label>
                <input id="model-name" name="modelName" value={form.modelName} onChange={updateField} placeholder="예: Galaxy S25+" aria-invalid={Boolean(fieldErrors.modelName)} />
                {fieldErrors.modelName && <p>{fieldErrors.modelName}</p>}
              </div>
              <div className={styles.field}>
                <label htmlFor="os-version">OS 버전 <span className={styles.optional}>선택</span></label>
                <input id="os-version" name="osVersion" value={form.osVersion} onChange={updateField} placeholder="예: Android 16" />
              </div>
              {form.serialNumber && (
                <div className={styles.field}>
                  <label htmlFor="serial-number">Serial <span className={styles.optional}>ADB 감지</span></label>
                  <input id="serial-number" name="serialNumber" value={form.serialNumber} readOnly />
                </div>
              )}
            </div>
            <div className={styles.formFooter}>
              <button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button>
              <button className={styles.primaryButton} type="submit" disabled={isSaving}>{isSaving ? '저장 중...' : mode === 'create' ? '등록하기' : '변경사항 저장'}</button>
            </div>
          </form>
        )}
      </aside>
    </div>
  )
}
