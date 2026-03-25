import { MapPin } from 'lucide-react';

export default function Zones() {
  return (
    <div style={{ padding: '32px' }}>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}><MapPin color="#3b82f6" /> Zonas Restringidas Avanzadas</h1>
        <p style={{ margin: '8px 0 0 0', color: '#6b7280' }}>Gestión completa de zonas restringidas en pantalla completa.</p>
      </header>
      
      <div style={{ background: 'white', padding: '40px', borderRadius: '12px', border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
        <h2>Configuración Global de Zonas</h2>
        <p>Aquí se podrá configurar radios específicos, horas de alerta o subir listados bulk en JSON.</p>
        <p>La vista rápida ya está integrada en el Dashboard Principal.</p>
      </div>
    </div>
  );
}
