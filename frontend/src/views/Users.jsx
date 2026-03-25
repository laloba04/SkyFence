import { Users } from 'lucide-react';

export default function UsersView() {
  return (
    <div style={{ padding: '32px' }}>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}><Users color="#3b82f6" /> Administración de Usuarios</h1>
        <p style={{ margin: '8px 0 0 0', color: '#6b7280' }}>Administración de cuentas, permisos (RBAC) y roles.</p>
      </header>
      
      <div style={{ background: 'white', padding: '40px', borderRadius: '12px', border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
        <h2>Gestión de Cuentas (Proximamente)</h2>
        <p>Aquí se gestionarán los controladores aéreos, técnicos o administradores cuando se integre Spring Security.</p>
      </div>
    </div>
  );
}
