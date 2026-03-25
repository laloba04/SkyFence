import { Bell } from 'lucide-react';

export default function Alerts() {
  return (
    <div style={{ padding: '32px' }}>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}><Bell color="#3b82f6" /> Histórico de Alertas</h1>
        <p style={{ margin: '8px 0 0 0', color: '#6b7280' }}>Consulta el registro histórico de contingencias guardado en la Base de Datos.</p>
      </header>
      
      <div style={{ background: 'white', padding: '40px', borderRadius: '12px', border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
        <Bell size={48} color="#d1d5db" style={{ marginBottom: '16px' }} />
        <h2>Próximamente...</h2>
        <p>Esta funcionalidad estará disponible cuando se implemente la persistencia del histórico de alertas en la base de datos.</p>
      </div>
    </div>
  );
}
