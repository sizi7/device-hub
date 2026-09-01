import Icon from '../../components/common/Icon.jsx'
import styles from './DeleteDialog.module.css'

export default function DeleteDialog({ device, isDeleting, onCancel, onConfirm }) {
  return (
    <div className={styles.layer} role="presentation">
      <div className={styles.backdrop} />
      <section className={styles.dialog} role="alertdialog" aria-modal="true" aria-labelledby="delete-title" aria-describedby="delete-description">
        <span className={styles.icon}><Icon name="alert" size={23} /></span>
        <h2 id="delete-title">기기를 삭제하시겠습니까?</h2>
        <p id="delete-description"><strong>{device.name}</strong> 기기 정보가 영구적으로 삭제됩니다. 이 작업은 되돌릴 수 없습니다.</p>
        <div className={styles.actions}>
          <button className={styles.cancelButton} type="button" onClick={onCancel} disabled={isDeleting}>취소</button>
          <button className={styles.deleteButton} type="button" onClick={onConfirm} disabled={isDeleting}>{isDeleting ? '삭제 중...' : '삭제'}</button>
        </div>
      </section>
    </div>
  )
}
