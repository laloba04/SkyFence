import { useState, useEffect } from 'react';
import axios from 'axios';
import { Users, Trash2, ShieldCheck, UserCog, AlertTriangle, RefreshCw } from 'lucide-react';
import { authHeader, getUser } from '../auth';

const API = import.meta.env.VITE_API_URL;

const ROLE_STYLES = {
  ADMIN:    { bg: '#ede9fe', color: '#6d28d9', label: 'Admin',    icon: <ShieldCheck size={13} /> },
  OPERATOR: { bg: '#e0f2fe', color: '#0369a1', label: 'Operator', icon: <UserCog size={13} /> },
};

export default function UsersView() {
  const [users, setUsers]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [deleting, setDeleting] = useState(null);
  const currentUser = getUser();

  const fetchUsers = () => {
    setLoading(true);
    setError(null);
    axios.get(`${API}/api/users`, { headers: authHeader() })
      .then(r => setUsers(r.data))
      .catch(() => setError('No se pudo cargar la lista de usuarios. ¿Tienes permisos de administrador?'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchUsers(); }, []);

  const handleDelete = async (user) => {
    if (!window.confirm(`¿Eliminar al usuario "${user.username}"? Esta acción no se puede deshacer.`)) return;
    setDeleting(user.id);
    try {
      await axios.delete(`${API}/api/users/${user.id}`, { headers: authHeader() });
      setUsers(prev => prev.filter(u => u.id !== user.id));
    } catch {
      setError('Error al eliminar el usuario.');
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div style={{ padding: '32px', maxWidth: '900px', margin: '0 auto' }}>
      <header style={{ marginBottom: '28px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Users color="#3b82f6" /> Administración de Usuarios
          </h1>
          <p style={{ margin: '6px 0 0 0', color: '#6b7280' }}>
            Gestión de cuentas y roles del sistema.
          </p>
        </div>
        <button onClick={fetchUsers} disabled={loading}
          style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px',
                   background: '#3b82f6', color: 'white', border: 'none', cursor: 'pointer', fontWeight: 500, fontSize: '14px' }}>
          <RefreshCw size={15} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
          Actualizar
        </button>
      </header>

      {error && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '14px 16px',
                      background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: '10px',
                      color: '#991b1b', marginBottom: '20px' }}>
          <AlertTriangle size={18} /> {error}
        </div>
      )}

      {loading ? (
        <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{ padding: '16px', borderBottom: '1px solid #f3f4f6', display: 'flex', gap: '16px', alignItems: 'center' }}>
              <div style={{ width: 120, height: 18, background: '#f3f4f6', borderRadius: 4, animation: 'skeletonPulse 1.5s ease-in-out infinite', animationDelay: `${i * 0.15}s` }} />
              <div style={{ width: 70, height: 22, background: '#f3f4f6', borderRadius: 9999, animation: 'skeletonPulse 1.5s ease-in-out infinite', animationDelay: `${i * 0.15 + 0.05}s` }} />
            </div>
          ))}
        </div>
      ) : !error && users.length === 0 ? (
        <div style={{ background: 'white', padding: '56px 40px', borderRadius: '12px',
                      border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
          <Users size={44} color="#d1d5db" style={{ marginBottom: '14px' }} />
          <h2 style={{ margin: '0 0 8px', color: '#374151' }}>Sin usuarios</h2>
          <p style={{ margin: 0 }}>No hay cuentas registradas en el sistema.</p>
        </div>
      ) : (
        <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>ID</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Usuario</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Rol</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u, i) => {
                const style = ROLE_STYLES[u.role] || ROLE_STYLES.OPERATOR;
                const isSelf = currentUser?.username === u.username;
                return (
                  <tr key={u.id} style={{ borderBottom: '1px solid #f3f4f6', background: i % 2 === 0 ? 'white' : '#fafafa' }}>
                    <td style={{ padding: '12px 16px', color: '#9ca3af', fontFamily: 'monospace' }}>#{u.id}</td>
                    <td style={{ padding: '12px 16px', color: '#111827', fontWeight: 500 }}>
                      {u.username}
                      {isSelf && <span style={{ marginLeft: 8, fontSize: 11, color: '#6b7280', background: '#f3f4f6', padding: '1px 6px', borderRadius: 4 }}>tú</span>}
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '3px 10px',
                                     borderRadius: 9999, fontSize: 12, fontWeight: 600,
                                     background: style.bg, color: style.color }}>
                        {style.icon} {style.label}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px' }}>
                      {!isSelf && (
                        <button
                          onClick={() => handleDelete(u)}
                          disabled={deleting === u.id}
                          title="Eliminar usuario"
                          style={{ display: 'inline-flex', alignItems: 'center', gap: 5,
                                   padding: '6px 12px', borderRadius: 8, fontSize: 13,
                                   border: '1px solid #fca5a5', background: '#fef2f2',
                                   color: '#dc2626', cursor: 'pointer', fontWeight: 500,
                                   opacity: deleting === u.id ? 0.6 : 1 }}>
                          <Trash2 size={13} />
                          {deleting === u.id ? 'Eliminando…' : 'Eliminar'}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes skeletonPulse { 0%,100%{opacity:1}50%{opacity:.4} }
      `}</style>
    </div>
  );
}
