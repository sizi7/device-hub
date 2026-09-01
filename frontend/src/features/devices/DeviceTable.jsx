import Icon from '../../components/common/Icon.jsx'
import styles from './DeviceTable.module.css'

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(value))
}

function getLocation(device) {
  const deployment = device.currentDeployment
  if (!deployment) return '사내'
  const suffix = deployment.deploymentType === 'HOSPITAL_LOAN' ? '대여' : '전용'
  return `${deployment.hospitalName} ${suffix}`
}

function TableState({ icon, title, description, action }) {
  return (
    <div className={styles.state}>
      <span className={styles.stateIcon}><Icon name={icon} size={23} /></span>
      <h3>{title}</h3>
      <p>{description}</p>
      {action}
    </div>
  )
}

export default function DeviceTable({ devices, isLoading, error, hasSearch, onRetry, onCreate, onDetail, onEdit, onDelete }) {
  if (isLoading) {
    return (
      <div className={styles.loading} aria-live="polite">
        <span className={styles.spinner} />
        <p>기기 목록을 불러오는 중입니다.</p>
      </div>
    )
  }

  if (error) {
    return <TableState icon="alert" title="목록을 불러오지 못했습니다" description={error} action={<button className={styles.stateButton} type="button" onClick={onRetry}>다시 시도</button>} />
  }

  if (devices.length === 0) {
    return hasSearch
      ? <TableState icon="search" title="검색 결과가 없습니다" description="다른 이름, 제조사 또는 모델명으로 검색해 보세요." />
      : <TableState icon="inbox" title="등록된 기기가 없습니다" description="첫 번째 업무용 기기를 등록해 목록을 시작하세요." action={<button className={styles.stateButton} type="button" onClick={onCreate}>기기 등록</button>} />
  }

  return (
    <div className={styles.tableScroll}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th>이름</th>
            <th>모델명</th>
            <th>프로젝트</th>
            <th>위치/상태</th>
            <th>OS 버전</th>
            <th>등록일</th>
            <th className={styles.actionHeader}>작업</th>
          </tr>
        </thead>
        <tbody>
          {devices.map((device) => (
            <tr key={device.id}>
              <td className={styles.nameCell}><button className={styles.nameButton} type="button" onClick={() => onDetail(device)} title={device.name}>{device.name}</button></td>
              <td className={styles.textCell} title={device.modelName}>{device.modelName}</td>
              <td>{device.projectCount || 0}개</td>
              <td className={styles.textCell} title={getLocation(device)}>{getLocation(device)}</td>
              <td className={styles.textCell} title={device.osVersion || undefined}>{device.osVersion || <span className={styles.muted}>미입력</span>}</td>
              <td>{formatDate(device.createdAt)}</td>
              <td>
                <div className={styles.actions}>
                  <button type="button" onClick={() => onDetail(device)}>상세</button>
                  <button type="button" onClick={() => onEdit(device)}>수정</button>
                  <button className={styles.deleteButton} type="button" onClick={() => onDelete(device)}>삭제</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
