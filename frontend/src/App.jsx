import { useEffect, useState } from 'react'
import AppShell from './components/layout/AppShell.jsx'
import DevicesPage from './features/devices/DevicesPage.jsx'
import PlaceholderPage from './components/common/PlaceholderPage.jsx'
import ProjectsPage from './features/projects/ProjectsPage.jsx'
import LoginPage from './features/auth/LoginPage.jsx'
import { authApi, getCurrentUser, getToken, onAuthCleared } from './api/authApi.js'

const pageDescriptions = {
  Dashboard: '서비스 현황을 한눈에 확인하는 공간입니다.',
  Apps: '연결된 애플리케이션을 관리하는 공간입니다.',
  Users: 'DeviceHub 사용자를 관리하는 공간입니다.',
  Settings: '서비스 환경을 설정하는 공간입니다.',
}

export default function App() {
  const [activePage, setActivePage] = useState('Devices')
  const [user, setUser] = useState(() => (getToken() ? getCurrentUser() : null))

  // API 응답이 401이면 인터셉터가 인증 상태를 지우고, 여기서 로그인 화면으로 되돌린다.
  useEffect(() => onAuthCleared(() => setUser(null)), [])

  if (!user) return <LoginPage onLoggedIn={setUser} />

  return (
    <AppShell
      activePage={activePage}
      onNavigate={setActivePage}
      user={user}
      onLogout={() => {
        authApi.logout()
        setUser(null)
      }}
    >
      {activePage === 'Devices' ? (
        <DevicesPage />
      ) : activePage === 'Projects' ? (
        <ProjectsPage user={user} />
      ) : (
        <PlaceholderPage title={activePage} description={pageDescriptions[activePage]} />
      )}
    </AppShell>
  )
}
