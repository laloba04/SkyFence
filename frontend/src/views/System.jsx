import { useState, useEffect } from 'react';
import axios from 'axios';
import { Activity, Database, Cloud, Radio } from 'lucide-react';

export default function System() {
  const [health, setHealth] = useState(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    const fetchHealth = () => {
      axios.get(`${import.meta.env.VITE_API_URL}/actuator/health`)
        .then(res => { setHealth(res.data); setError(false); })
        .catch(err => { console.error(err); setError(true); });
    };
    fetchHealth();
    const interval = setInterval(fetchHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  const ComponentCard = ({ name, icon: Icon, statusDetails, isUp }) => (
    <div style={{ background: 'white', padding: '24px', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', borderTop: `4px solid ${isUp ? '#10b981' : '#ef4444'}` }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
        <div style={{ background: isUp ? '#d1fae5' : '#fee2e2', padding: '12px', borderRadius: '50%' }}>
          <Icon size={24} color={isUp ? '#10b981' : '#ef4444'} />
        </div>
        <div>
          <h3 style={{ margin: 0, fontSize: '18px', color: '#1f2937' }}>{name}</h3>
          <span style={{ fontSize: '14px', color: isUp ? '#10b981' : '#ef4444', fontWeight: 'bold' }}>{isUp ? 'OPERATIVO' : 'FALLO / NO DISPONIBLE'}</span>
        </div>
      </div>
      <pre style={{ background: '#111827', color: '#a7f3d0', padding: '16px', borderRadius: '8px', fontSize: '13px', overflowX: 'auto', margin: 0 }}>
        {JSON.stringify(statusDetails, null, 2)}
      </pre>
    </div>
  );

  return (
    <div style={{ padding: '32px' }}>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}><Activity color="#3b82f6" /> Salud del Sistema</h1>
        <p style={{ margin: '8px 0 0 0', color: '#6b7280' }}>Monitorización en tiempo real de los servicios y dependencias (Spring Actuator).</p>
      </header>

      {error ? (
        <div style={{ background: '#fee2e2', color: '#991b1b', padding: '16px', borderRadius: '8px', fontWeight: '500' }}>⚠️ Error conectando con el backend de Actuator en :8080. Servidor apagado o inaccesible.</div>
      ) : !health ? (
        <p style={{ color: '#6b7280' }}>Cargando telemetría de salud...</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '24px' }}>
          <ComponentCard name="Backend Principal (SkyFence API)" icon={Activity} isUp={health.status === 'UP'} statusDetails={{ status: health.status }} />
          <ComponentCard name="Base de Datos (PostgreSQL)" icon={Database} isUp={health.components?.db?.status === 'UP'} statusDetails={health.components?.db?.details || {}} />
          <ComponentCard name="adsb.fi API" icon={Cloud} isUp={health.components?.adsbfi?.status === 'UP'} statusDetails={health.components?.adsbfi?.details || {}} />
          <ComponentCard name="Broker STOMP (WebSockets)" icon={Radio} isUp={health.components?.websocket?.status === 'UP'} statusDetails={health.components?.websocket?.details || {}} />
        </div>
      )}
    </div>
  );
}
