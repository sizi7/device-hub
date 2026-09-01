import { useEffect, useState } from 'react'
import Icon from '../../components/common/Icon.jsx'
import styles from './ConnectedDeviceDialog.module.css'

function DeviceSummary({ device, selectable, selected, onSelect }) {
  const content = (
    <>
      <strong>{device.modelName}</strong>
      <span>{device.manufacturer} · Android {device.osVersion}</span>
      <span className={styles.serial}>{device.serialNumber}</span>
      {device.registeredDevice && <span>등록됨: {device.registeredDevice.name}</span>}
    </>
  )
  if (!selectable) return <div className={styles.deviceSummary}>{content}</div>
  return (
    <label className={[styles.deviceOption, selected ? styles.selected : ''].join(' ')}>
      <input type="radio" name="connected-device" checked={selected} onChange={onSelect} />
      <span>{content}</span>
    </label>
  )
}

export default function ConnectedDeviceDialog({ isLoading, result, error, onRetry, onManual, onUseDevice, onClose }) {
  const [selectedSerial, setSelectedSerial] = useState('')
  const status = result?.status
  const multipleDevices = result?.devices || []
  const selectedDevice = multipleDevices.find((device) => device.serialNumber === selectedSerial)

  useEffect(() => {
    if (multipleDevices.length > 0 && !selectedSerial) setSelectedSerial(multipleDevices[0].serialNumber)
  }, [multipleDevices, selectedSerial])

  let title = '연결 기기 검색'
  let description = ''
  let body = null
  let actions = null

  if (isLoading) {
    description = '현재 PC에 ADB로 연결된 Android 기기를 확인하고 있습니다.'
    body = <div className={styles.loading} aria-live="polite"><span className={styles.spinner} />기기 정보를 불러오는 중입니다.</div>
    actions = <button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button>
  } else if (error || status === 'ERROR' || status === 'ADB_NOT_AVAILABLE') {
    title = status === 'ADB_NOT_AVAILABLE' ? 'ADB를 사용할 수 없습니다' : '기기 검색에 실패했습니다'
    description = error || result?.message || 'ADB 실행 상태를 확인해 주세요.'
    body = <div className={styles.guide}><Icon name="alert" size={20} /><p>{description}</p></div>
    actions = <><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="button" onClick={onRetry}>다시 검색</button></>
  } else if (status === 'CONNECTED') {
    title = '연결된 기기를 찾았습니다'
    description = '기기 정보를 확인한 뒤 등록을 계속하세요.'
    body = <DeviceSummary device={result.device} />
    actions = <><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="button" onClick={() => onUseDevice(result.device)}>기기 등록</button></>
  } else if (status === 'NOT_FOUND') {
    title = '연결된 기기가 없습니다'
    description = 'USB 케이블과 USB 디버깅 상태를 확인하거나 직접 입력할 수 있습니다.'
    actions = <><button className={styles.secondaryButton} type="button" onClick={onManual}>직접 입력</button><button className={styles.primaryButton} type="button" onClick={onRetry}>다시 검색</button></>
  } else if (status === 'UNAUTHORIZED') {
    title = 'USB 디버깅 승인이 필요합니다'
    description = '기기 화면에서 연결을 승인해 주세요. 필요하면 “이 컴퓨터에서 항상 허용”을 선택한 뒤 다시 검색하세요.'
    actions = <><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="button" onClick={onRetry}>다시 검색</button></>
  } else if (status === 'OFFLINE') {
    title = 'ADB 통신이 불가능합니다'
    description = 'USB 케이블을 다시 연결하고 기기의 USB 디버깅 상태를 확인해 주세요.'
    actions = <><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="button" onClick={onRetry}>다시 검색</button></>
  } else if (status === 'MULTIPLE') {
    title = '연결된 기기를 선택하세요'
    description = '여러 기기 중 등록할 기기 한 대를 선택하세요.'
    body = <div className={styles.deviceList}>{multipleDevices.map((device) => <DeviceSummary key={device.serialNumber} device={device} selectable selected={device.serialNumber === selectedSerial} onSelect={() => setSelectedSerial(device.serialNumber)} />)}</div>
    actions = <><button className={styles.secondaryButton} type="button" onClick={onClose}>취소</button><button className={styles.primaryButton} type="button" disabled={!selectedDevice || selectedDevice.registeredDevice} onClick={() => onUseDevice(selectedDevice)}>선택</button></>
  } else if (status === 'ALREADY_REGISTERED') {
    title = '이미 등록된 기기입니다'
    description = 'Serial 번호가 같은 기기가 DeviceHub에 등록되어 있습니다.'
    body = <><DeviceSummary device={result.device} /><dl className={styles.registered}><div><dt>등록 이름</dt><dd>{result.registeredDevice?.name}</dd></div><div><dt>Device ID</dt><dd>#{result.registeredDevice?.id}</dd></div></dl></>
    actions = <button className={styles.primaryButton} type="button" onClick={onClose}>확인</button>
  }

  return (
    <div className={styles.layer} role="presentation">
      <div className={styles.backdrop} />
      <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="connected-title">
        <div className={styles.header}>
          <div><span>ADB device</span><h2 id="connected-title">{title}</h2><p>{description}</p></div>
          <button className={styles.closeButton} type="button" onClick={onClose} aria-label="닫기"><Icon name="close" size={20} /></button>
        </div>
        {body && <div className={styles.body}>{body}</div>}
        <div className={styles.actions}>{actions}</div>
      </section>
    </div>
  )
}
