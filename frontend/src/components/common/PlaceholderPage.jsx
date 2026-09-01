import styles from './PlaceholderPage.module.css'

export default function PlaceholderPage({ title, description }) {
  return (
    <section>
      <div className={styles.heading}>
        <p className={styles.eyebrow}>DeviceHub Admin</p>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      <div className={styles.placeholder}>
        <span>준비 중</span>
        <h2>{title} 화면은 다음 단계에서 제공됩니다.</h2>
        <p>현재는 Devices 메뉴에서 기기 관리 기능을 사용할 수 있습니다.</p>
      </div>
    </section>
  )
}
