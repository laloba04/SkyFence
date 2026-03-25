import { ShieldCheck } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export default function Login() {
  const navigate = useNavigate();

  const handleSimulateLogin = (e) => {
    e.preventDefault();
    navigate('/dashboard');
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#f3f4f6' }}>
      <div style={{ background: 'white', padding: '40px', borderRadius: '16px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', width: '100%', maxWidth: '400px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px', marginBottom: '32px' }}>
          <div style={{ background: '#eff6ff', padding: '16px', borderRadius: '50%' }}>
            <ShieldCheck size={48} color="#3b82f6" />
          </div>
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 'bold' }}>SkyFence Security</h1>
          <p style={{ margin: 0, color: '#6b7280', fontSize: '14px' }}>Inicia sesión para acceder al centro de control</p>
        </div>

        <form onSubmit={handleSimulateLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Usuario o Email</label>
            <input type="text" placeholder="admin@skyfence.local" style={{ padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none' }} />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Contraseña</label>
            <input type="password" placeholder="••••••••" style={{ padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none' }} />
          </div>
          <button type="submit" style={{ background: '#3b82f6', color: 'white', border: 'none', padding: '14px', borderRadius: '8px', fontWeight: 'bold', cursor: 'pointer', transition: 'background 0.2s', marginTop: '10px' }}
            onMouseOver={e => e.currentTarget.style.background = '#2563eb'}
            onMouseOut={e => e.currentTarget.style.background = '#3b82f6'}
          >
            Entrar al Centro de Mando
          </button>
        </form>
        <p style={{ textAlign: 'center', fontSize: '12px', color: '#9ca3af', marginTop: '24px' }}>La autenticación real estará disponible cuando se integre el sistema de seguridad JWT en el servidor.</p>
      </div>
    </div>
  );
}
