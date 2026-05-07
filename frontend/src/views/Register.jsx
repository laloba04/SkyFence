import { useState } from 'react';
import { ShieldCheck } from 'lucide-react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export default function Register() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '', confirm: '', role: 'OPERATOR' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleChange = e => setForm(f => ({ ...f, [e.target.name]: e.target.value }));

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password !== form.confirm) {
      setError('Las contraseñas no coinciden');
      return;
    }
    setLoading(true);
    try {
      await axios.post(`${API}/api/auth/register`, {
        username: form.username,
        password: form.password,
        role: form.role,
      });
      navigate('/login', { state: { registered: true } });
    } catch (err) {
      setError(err.response?.data?.error || 'Error al registrar el usuario');
    } finally {
      setLoading(false);
    }
  };

  const inputStyle = { padding: '12px', borderRadius: '8px', border: '1px solid #d1d5db', outline: 'none', fontFamily: 'inherit' };

  return (
    <div style={{ minHeight: '100vh', background: '#0f172a', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <div style={{ background: 'white', borderRadius: '16px', padding: '48px', width: '100%', maxWidth: '420px', boxShadow: '0 20px 60px rgba(0,0,0,0.3)' }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <ShieldCheck size={48} color="#3b82f6" style={{ marginBottom: '12px' }} />
          <h1 style={{ margin: 0, fontSize: '24px', fontWeight: 'bold', color: '#1f2937' }}>SkyFence</h1>
          <p style={{ margin: '8px 0 0', color: '#6b7280', fontSize: '14px' }}>Crea tu cuenta de operador</p>
        </div>

        <form onSubmit={handleRegister} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Usuario</label>
            <input name="username" value={form.username} onChange={handleChange} placeholder="nombre_usuario" required style={inputStyle} />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Contraseña</label>
            <input name="password" type="password" value={form.password} onChange={handleChange} placeholder="••••••••" required style={inputStyle} />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Confirmar contraseña</label>
            <input name="confirm" type="password" value={form.confirm} onChange={handleChange} placeholder="••••••••" required style={inputStyle} />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            <label style={{ fontSize: '14px', fontWeight: '600', color: '#374151' }}>Rol</label>
            <select name="role" value={form.role} onChange={handleChange} style={{ ...inputStyle, background: 'white', cursor: 'pointer' }}>
              <option value="OPERATOR">Operador</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </div>

          {error && <p style={{ margin: 0, color: '#ef4444', fontSize: '14px', textAlign: 'center' }}>{error}</p>}

          <button
            type="submit"
            disabled={loading}
            style={{ background: loading ? '#93c5fd' : '#3b82f6', color: 'white', border: 'none', padding: '14px', borderRadius: '8px', fontWeight: 'bold', cursor: loading ? 'default' : 'pointer', marginTop: '6px' }}
            onMouseOver={e => { if (!loading) e.currentTarget.style.background = '#2563eb'; }}
            onMouseOut={e => { if (!loading) e.currentTarget.style.background = '#3b82f6'; }}
          >
            {loading ? 'Registrando…' : 'Crear cuenta'}
          </button>
        </form>

        <p style={{ textAlign: 'center', fontSize: '14px', color: '#6b7280', marginTop: '24px' }}>
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" style={{ color: '#3b82f6', fontWeight: '600', textDecoration: 'none' }}>Iniciar sesión</Link>
        </p>
      </div>
    </div>
  );
}
