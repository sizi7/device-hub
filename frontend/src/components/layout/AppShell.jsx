import Icon from '../common/Icon.jsx'
import styles from './AppShell.module.css'

const navigation = [
  { label: 'Dashboard', icon: 'dashboard' },
  { label: 'Devices', icon: 'devices', ready: true },
  { label: 'Projects', icon: 'apps', ready: true },
  { label: 'Apps', icon: 'apps' },
  { label: 'Users', icon: 'users', ready: true, adminOnly: true },
  { label: 'Settings', icon: 'settings' },
]

const roleLabels = {
  ROLE_ADMIN: '관리자',
  ROLE_RELEASE_MANAGER: '배포 담당자',
  ROLE_USER: '일반 사용자',
}

export default function AppShell({ activePage, onNavigate, user, onLogout, onChangePassword, children }) {
  // 관리자 전용 메뉴는 권한이 없으면 목록에서 감춘다. 실제 차단은 백엔드 @PreAuthorize가 담당한다.
  const menu = navigation.filter((item) => !item.adminOnly || user?.role === 'ROLE_ADMIN')

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.brand}>
          <span className={styles.brandMark}><Icon name="devices" size={21} /></span>
          <span>DeviceHub</span>
        </div>
        <nav className={styles.navigation} aria-label="주 메뉴">
          {menu.map((item) => (
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
            <span className={styles.avatar}>{(user?.name || user?.username || '?').slice(0, 2)}</span>
            <div>
              <strong>{user?.name || user?.username}</strong>
              <span>{roleLabels[user?.role] || user?.role}</span>
            </div>
            <button className={styles.logout} type="button" onClick={onChangePassword}>비밀번호 변경</button>
            <button className={styles.logout} type="button" onClick={onLogout}>로그아웃</button>
          </div>
        </header>
        <main className={styles.main}>{children}</main>
      </div>
    </div>
  )
}
