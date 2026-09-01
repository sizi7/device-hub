import Icon from '../common/Icon.jsx'
import styles from './AppShell.module.css'

const navigation = [
  { label: 'Dashboard', icon: 'dashboard' },
  { label: 'Devices', icon: 'devices', ready: true },
  { label: 'Projects', icon: 'apps', ready: true },
  { label: 'Apps', icon: 'apps' },
  { label: 'Users', icon: 'users' },
  { label: 'Settings', icon: 'settings' },
]

export default function AppShell({ activePage, onNavigate, children }) {
  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandMark}><Icon name="devices" size={21} /></span>
          <span>DeviceHub</span>
        </div>
        <nav className={styles.navigation} aria-label="주 메뉴">
          {navigation.map((item) => (
            <button
              className={`${styles.navItem} ${activePage === item.label ? styles.active : ''}`}
              key={item.label}
              type="button"
              onClick={() => onNavigate(item.label)}
            >
              <Icon name={item.icon} size={19} />
              <span>{item.label}</span>
              {!item.ready && <span className={styles.soon}>준비 중</span>}
            </button>
          ))}
        </nav>
        <div className={styles.sidebarFooter}>
          <span className={styles.environmentDot} />
          <div>
            <strong>Development</strong>
            <span>API 연결됨</span>
          </div>
        </div>
      </aside>

      <div className={styles.workspace}>
        <header className={styles.header}>
          <div>
            <span className={styles.headerSection}>관리자 콘솔</span>
            <span className={styles.headerDivider}>/</span>
            <strong>{activePage}</strong>
          </div>
          <div className={styles.profile}>
            <span className={styles.avatar}>AD</span>
            <div>
              <strong>Administrator</strong>
              <span>admin@devicehub.local</span>
            </div>
          </div>
        </header>
        <main className={styles.main}>{children}</main>
      </div>
    </div>
  )
}
