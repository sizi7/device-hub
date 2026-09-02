import { useEffect, useState } from 'react'
import AppShell from './components/layout/AppShell.jsx'
import DevicesPage from './features/devices/DevicesPage.jsx'
import PlaceholderPage from './components/common/PlaceholderPage.jsx'
import ProjectsPage from './features/projects/ProjectsPage.jsx'
import UsersPage from './features/users/UsersPage.jsx'
import LoginPage from './features/auth/LoginPage.jsx'
import PasswordChangeDialog from './features/auth/PasswordChangeDialog.jsx'
import { authApi, getCurrentUser, getToken, isAdmin, onAuthCleared } from './api/authApi.js'

const pageDescriptions = {
  Dashboard: '서비스 현황을 한눈에 확인하는 공간입니다.',
  Apps: '연결된 애플리케이션을 관리하는 공간입니다.',
  Settings: '서비스 환경을 설정하는 공간입니다.',
  Users: 'DeviceHub 사용자를 관리하는 공간입니다. 관리자만 이용할 수 있습니다.',
}

export default function App() {
  const [activePage, setActivePage] = useState('Devices')
  const [user, setUser] = useState(() => (getToken() ? getCurrentUser() : null))
  const [passwordDialog, setPasswordDialog] = useState(false)

  // API 응답이 401이면 인터셉터가 인증 상태를 지우고, 여기서 로그인 화면으로 되돌린다.
  useEffect(() => onAuthCleared(() => setUser(null)), [])

  if (!user) return <LoginPage onLoggedIn={setUser} />

  function renderPage() {
    if (activePage === 'Devices') return <DevicesPage />
    if (activePage === 'Projects') return <ProjectsPage user={user} />
    // 화면을 감추는 것은 편의를 위한 처리이고 실제 차단은 백엔드 @PreAuthorize가 담당한다.
    if (activePage === 'Users') {
      return isAdmin(user)
        ? <UsersPage user={user} />
        : <PlaceholderPage title="Users" description="사용자 관리는 관리자만 이용할 수 있습니다." />
    }
    return <PlaceholderPage title={activePage} description={pageDescriptions[activePage]} />
  }

  return (
    <>
      <AppShell
        activePage={activePage}
        onNavigate={setActivePage}
        user={user}
        onLogout={() => {
          authApi.logout()
          setUser(null)
        }}
        onChangePassword={() => setPasswordDialog(true)}
      >
        {renderPage()}
      </AppShell>
      {passwordDialog && <PasswordChangeDialog onClose={() => setPasswordDialog(false)} />}
    </>
  )
}
