import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout';
import PrivateRoute from './components/PrivateRoute';
import Dashboard from './views/Dashboard';
import Alerts from './views/Alerts';
import Zones from './views/Zones';
import System from './views/System';
import UsersView from './views/Users';
import Login from './views/Login';
import Register from './views/Register';
import { getUser } from './auth';

function AdminRoute({ children }) {
  const user = getUser();
  return user?.role === 'ADMIN' ? children : <Navigate to="/dashboard" replace />;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route element={<PrivateRoute><Layout /></PrivateRoute>}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/alerts" element={<Alerts />} />
          <Route path="/zones" element={<Zones />} />
          <Route path="/system" element={<System />} />
          <Route path="/users" element={<AdminRoute><UsersView /></AdminRoute>} />

          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
