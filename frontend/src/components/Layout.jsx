import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { ShieldCheck, Map, Bell, MapPin, Activity, Users, LogOut } from 'lucide-react';
import { clearAuth, getUser } from '../auth';

export default function Layout() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = getUser();
  const isAdmin = user?.role === 'ADMIN';

  const navItems = [
    { name: 'Dashboard Real-Time', path: '/dashboard', icon: Map },
    { name: 'Histórico Alertas', path: '/alerts', icon: Bell },
    { name: 'Zonas Restringidas', path: '/zones', icon: MapPin },
    { name: 'Salud del Sistema', path: '/system', icon: Activity },
    ...(isAdmin ? [{ name: 'Usuarios y Control', path: '/users', icon: Users }] : []),
  ];

  const handleLogout = () => {
    clearAuth();
    navigate('/login');
  };

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', background: '#f5f7fa', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      {/* Sidebar Oscuro y Premium */}
      <aside style={{ width: '280px', flexShrink: 0, background: '#111827', color: 'white', display: 'flex', flexDirection: 'column', boxShadow: '4px 0 10px rgba(0,0,0,0.1)', zIndex: 10, overflowY: 'auto' }}>
        
        {/* Logo/Header */}
        <div style={{ padding: '24px', display: 'flex', alignItems: 'center', gap: '12px', borderBottom: '1px solid #1f2937' }}>
          <ShieldCheck size={32} color="#3b82f6" />
          <div>
            <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', letterSpacing: '0.5px' }}>SkyFence</h1>
            <span style={{ fontSize: '12px', color: '#9ca3af' }}>Intrusion Detection System</span>
          </div>
        </div>

        {/* Navigation */}
        <nav style={{ padding: '24px 16px', flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              style={({ isActive }) => ({
                display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', borderRadius: '8px',
                textDecoration: 'none', fontSize: '14px', fontWeight: '500', transition: 'all 0.2s',
                background: isActive ? '#1f2937' : 'transparent',
                color: isActive ? '#3b82f6' : '#9ca3af',
                borderLeft: isActive ? '4px solid #3b82f6' : '4px solid transparent'
              })}
            >
              <item.icon size={20} />
              {item.name}
            </NavLink>
          ))}
        </nav>

        {/* Footer/Logout */}
        <div style={{ padding: '24px 16px', borderTop: '1px solid #1f2937' }}>
          <button 
            onClick={handleLogout}
            style={{ width: '100%', display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', background: 'transparent', border: 'none', color: '#ef4444', fontSize: '14px', fontWeight: '500', cursor: 'pointer', borderRadius: '8px', transition: 'background 0.2s' }}
            onMouseOver={e => e.currentTarget.style.background = 'rgba(239, 68, 68, 0.1)'}
            onMouseOut={e => e.currentTarget.style.background = 'transparent'}
          >
            <LogOut size={20} />
            Cerrar Sesión {user ? `(${user.username})` : ''}
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <main style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden' }}>
        <style>{`@keyframes fadeIn { from{opacity:0;transform:translateY(4px)}to{opacity:1;transform:translateY(0)} }`}</style>
        <div key={location.pathname} style={{ animation: 'fadeIn 0.18s ease-out' }}>
          <Outlet />
        </div>
      </main>
    </div>
  );
}
