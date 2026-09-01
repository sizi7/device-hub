import { useCallback, useEffect, useMemo, useState } from 'react'
import Icon from '../../components/common/Icon.jsx'
import { deviceApi, getApiErrorMessage } from '../../api/deviceApi.js'
import DeviceTable from './DeviceTable.jsx'
import DeviceDrawer from './DeviceDrawer.jsx'
import DeleteDialog from './DeleteDialog.jsx'
import ConnectedDeviceDialog from './ConnectedDeviceDialog.jsx'
import styles from './DevicesPage.module.css'

export default function DevicesPage() {
  const [devices, setDevices] = useState([])
  const [searchTerm, setSearchTerm] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [drawer, setDrawer] = useState(null)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [detection, setDetection] = useState(null)

  const loadDevices = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      setDevices(await deviceApi.getAll())
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDevices()
  }, [loadDevices])

  const filteredDevices = useMemo(() => {
    const query = searchTerm.trim().toLowerCase()
    if (!query) return devices
    return devices.filter((device) =>
      [device.name, device.type, device.manufacturer, device.modelName, device.osVersion]
        .filter(Boolean)
        .some((value) => value.toLowerCase().includes(query)),
    )
  }, [devices, searchTerm])

  const handleSaved = (savedDevice) => {
    setDevices((current) => {
      const exists = current.some((device) => device.id === savedDevice.id)
      return exists
        ? current.map((device) => (device.id === savedDevice.id ? savedDevice : device))
        : [savedDevice, ...current]
    })
    setDrawer(null)
  }

  const handleDelete = async () => {
    if (!deleteTarget) return
    setIsDeleting(true)
    try {
      await deviceApi.remove(deleteTarget.id)
      setDevices((current) => current.filter((device) => device.id !== deleteTarget.id))
      setDeleteTarget(null)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError))
      setDeleteTarget(null)
    } finally {
      setIsDeleting(false)
    }
  }

  const detectConnectedDevice = async () => {
    setDetection({ isLoading: true, result: null, error: '' })
    try {
      const result = await deviceApi.getConnected()
      setDetection({ isLoading: false, result, error: '' })
    } catch (requestError) {
      setDetection({ isLoading: false, result: null, error: getApiErrorMessage(requestError) })
    }
  }

  const openManualCreate = () => {
    setDetection(null)
    setDrawer({ mode: 'create' })
  }

  const openDetectedCreate = (device) => {
    setDetection(null)
    setDrawer({
      mode: 'create',
      device: {
        name: device.modelName,
        type: device.type || '',
        manufacturer: device.manufacturer,
        modelName: device.modelName,
        osVersion: device.osVersion,
        serialNumber: device.serialNumber,
      },
    })
  }

  return (
    <section>
      <div className={styles.pageHeading}>
        <div>
          <p className={styles.eyebrow}>Device management</p>
          <h1>Devices</h1>
          <p>등록된 업무용 기기와 운영체제 정보를 관리합니다.</p>
        </div>
        <button className={styles.primaryButton} type="button" onClick={detectConnectedDevice}>
          <Icon name="plus" size={18} />
          기기 등록
        </button>
      </div>

      <div className={styles.summaryBar}>
        <div>
          <span>전체 기기</span>
          <strong>{devices.length}</strong>
        </div>
        <div>
          <span>스마트폰</span>
          <strong>{devices.filter((device) => device.type === 'PHONE').length}</strong>
        </div>
        <div>
          <span>태블릿</span>
          <strong>{devices.filter((device) => device.type === 'TABLET').length}</strong>
        </div>
      </div>

      <div className={styles.contentPanel}>
        <div className={styles.toolbar}>
          <div>
            <h2>기기 목록</h2>
            <span>{filteredDevices.length}개 항목</span>
          </div>
          <div className={styles.toolbarActions}>
            <label className={styles.searchBox}>
              <Icon name="search" size={18} />
              <input
                aria-label="기기 검색"
                type="search"
                placeholder="이름, 제조사, 모델명 검색"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
              />
            </label>
            <button className={styles.refreshButton} type="button" onClick={loadDevices} disabled={isLoading} aria-label="목록 새로고침">
              <Icon name="refresh" size={18} />
            </button>
          </div>
        </div>

        <DeviceTable
          devices={filteredDevices}
          isLoading={isLoading}
          error={error}
          hasSearch={Boolean(searchTerm.trim())}
          onRetry={loadDevices}
          onCreate={detectConnectedDevice}
          onDetail={(device) => setDrawer({ mode: 'detail', device })}
          onEdit={(device) => setDrawer({ mode: 'edit', device })}
          onDelete={setDeleteTarget}
        />
      </div>

      {drawer && (
        <DeviceDrawer
          mode={drawer.mode}
          device={drawer.device}
          onClose={() => setDrawer(null)}
          onSaved={handleSaved}
        />
      )}

      {deleteTarget && (
        <DeleteDialog
          device={deleteTarget}
          isDeleting={isDeleting}
          onCancel={() => setDeleteTarget(null)}
          onConfirm={handleDelete}
        />
      )}

      {detection && (
        <ConnectedDeviceDialog
          isLoading={detection.isLoading}
          result={detection.result}
          error={detection.error}
          onRetry={detectConnectedDevice}
          onManual={openManualCreate}
          onUseDevice={openDetectedCreate}
          onClose={() => setDetection(null)}
        />
      )}
    </section>
  )
}
