import { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import axios from 'axios';
import { saveAuth } from '../auth';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const justRegistered = location.state?.registered;
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await axios.post(`${API}/api/auth/login`, { username, password });
      saveAuth(data.token, { username: data.username, role: data.role });
      navigate('/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Credenciales incorrectas');
    } finally {
      setLoading(false);
    }
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

        <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Usuario</label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="admin"
              required
              style={{ padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none' }}
            />
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              style={{ padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none' }}
            />
          </div>
          {justRegistered && (
            <p style={{ margin: 0, color: '#16a34a', fontSize: '14px', textAlign: 'center' }}>Cuenta creada. Inicia sesión.</p>
          )}
          {error && (
            <p style={{ margin: 0, color: '#ef4444', fontSize: '14px', textAlign: 'center' }}>{error}</p>
          )}
          <button
            type="submit"
            disabled={loading}
            style={{ background: loading ? '#93c5fd' : '#3b82f6', color: 'white', border: 'none', padding: '14px', borderRadius: '8px', fontWeight: 'bold', cursor: loading ? 'default' : 'pointer', marginTop: '10px' }}
            onMouseOver={e => { if (!loading) e.currentTarget.style.background = '#2563eb'; }}
            onMouseOut={e => { if (!loading) e.currentTarget.style.background = '#3b82f6'; }}
          >
            {loading ? 'Iniciando sesión…' : 'Entrar al Centro de Mando'}
          </button>
        </form>

        <p style={{ textAlign: 'center', fontSize: '14px', color: '#6b7280', marginTop: '24px' }}>
          ¿No tienes cuenta?{' '}
          <Link to="/register" style={{ color: '#3b82f6', fontWeight: '600', textDecoration: 'none' }}>Registrarse</Link>
        </p>
      </div>
    </div>
  );
}
