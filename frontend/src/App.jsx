import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import RequireAuth from './components/RequireAuth'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ForgotPasswordPage from './pages/ForgotPasswordPage'
import DashboardPage from './pages/DashboardPage'
import CreateCasePage from './pages/CreateCasePage'
import CaseDetailPage from './pages/CaseDetailPage'
import ProfilePage from './pages/ProfilePage'
import ManageUsersPage from './pages/ManageUsersPage'
import DistrictOverviewPage from './pages/DistrictOverviewPage'
import ConversationListPage from './pages/ConversationListPage'
import ChatRoomPage from './pages/ChatRoomPage'
import UserActivityPage from './pages/UserActivityPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route
            path="/"
            element={
              <RequireAuth>
                <DashboardPage />
              </RequireAuth>
            }
          />
          <Route
            path="/cases/new"
            element={
              <RequireAuth>
                <CreateCasePage />
              </RequireAuth>
            }
          />
          <Route
            path="/cases/:id"
            element={
              <RequireAuth>
                <CaseDetailPage />
              </RequireAuth>
            }
          />
          <Route
            path="/profile"
            element={
              <RequireAuth>
                <ProfilePage />
              </RequireAuth>
            }
          />
          <Route
            path="/users"
            element={
              <RequireAuth>
                <ManageUsersPage />
              </RequireAuth>
            }
          />
          <Route
            path="/users/:id/activity"
            element={
              <RequireAuth>
                <UserActivityPage />
              </RequireAuth>
            }
          />
          <Route
            path="/district-overview"
            element={
              <RequireAuth>
                <DistrictOverviewPage />
              </RequireAuth>
            }
          />
          <Route
            path="/messages"
            element={
              <RequireAuth>
                <ConversationListPage />
              </RequireAuth>
            }
          />
          <Route
            path="/messages/:conversationId"
            element={
              <RequireAuth>
                <ChatRoomPage />
              </RequireAuth>
            }
          />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
