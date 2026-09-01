import { useState } from 'react'
import AppShell from './components/layout/AppShell.jsx'
import DevicesPage from './features/devices/DevicesPage.jsx'
import PlaceholderPage from './components/common/PlaceholderPage.jsx'

const pageDescriptions = {
  Dashboard: '서비스 현황을 한눈에 확인하는 공간입니다.',
  Apps: '연결된 애플리케이션을 관리하는 공간입니다.',
  Users: 'DeviceHub 사용자를 관리하는 공간입니다.',
  Settings: '서비스 환경을 설정하는 공간입니다.',
}

export default function App() {
  const [activePage, setActivePage] = useState('Devices')

  return (
    <AppShell activePage={activePage} onNavigate={setActivePage}>
      {activePage === 'Devices' ? (
        <DevicesPage />
      ) : (
        <PlaceholderPage title={activePage} description={pageDescriptions[activePage]} />
      )}
    </AppShell>
  )
}
