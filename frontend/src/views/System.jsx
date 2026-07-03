import { useState, useEffect } from 'react';
import axios from 'axios';
import { Activity, Database, Cloud, Radio, BarChart3, ExternalLink } from 'lucide-react';

const GRAFANA_URL = import.meta.env.VITE_GRAFANA_URL;
const GRAFANA_DASHBOARD = `${GRAFANA_URL}/d/skyfence-backend/skyfence-backend`;

const SkeletonCard = ({ delay = 0 }) => (
  <div style={{ background: 'white', padding: '24px', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', borderTop: '4px solid #e5e7eb' }}>
    <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
      <div style={{ background: '#e5e7eb', width: '48px', height: '48px', borderRadius: '50%', flexShrink: 0, animation: `skeletonPulse 1.5s ease-in-out ${delay}s infinite` }} />
      <div style={{ flex: 1 }}>
        <div style={{ height: '18px', background: '#e5e7eb', borderRadius: '4px', marginBottom: '8px', width: '60%', animation: `skeletonPulse 1.5s ease-in-out ${delay}s infinite` }} />
        <div style={{ height: '14px', background: '#e5e7eb', borderRadius: '4px', width: '35%', animation: `skeletonPulse 1.5s ease-in-out ${delay}s infinite` }} />
      </div>
    </div>
    <div style={{ height: '72px', background: '#111827', borderRadius: '8px', opacity: 0.08, animation: `skeletonPulse 1.5s ease-in-out ${delay}s infinite` }} />
  </div>
);

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

  const getState = (status) => {
    if (status === 'UP')      return { color: '#10b981', bg: '#d1fae5', label: 'OPERATIVO' };
    if (status === 'UNKNOWN') return { color: '#f59e0b', bg: '#fef3c7', label: 'DEGRADADO' };
    return                           { color: '#ef4444', bg: '#fee2e2', label: 'FALLO / NO DISPONIBLE' };
  };

  const ComponentCard = ({ name, icon: Icon, statusDetails, status }) => {
    const state = getState(status);
    return (
      <div style={{ background: 'white', padding: '24px', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', borderTop: `4px solid ${state.color}` }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div style={{ background: state.bg, padding: '12px', borderRadius: '50%' }}>
            <Icon size={24} color={state.color} />
          </div>
          <div>
            <h3 style={{ margin: 0, fontSize: '18px', color: '#1f2937' }}>{name}</h3>
            <span style={{ fontSize: '14px', color: state.color, fontWeight: 'bold' }}>{state.label}</span>
          </div>
        </div>
        <pre style={{ background: '#111827', color: '#a7f3d0', padding: '16px', borderRadius: '8px', fontSize: '13px', overflowX: 'auto', margin: 0 }}>
          {JSON.stringify(statusDetails, null, 2)}
        </pre>
      </div>
    );
  };

  return (
    <div style={{ padding: '32px' }}>
      <style>{`@keyframes skeletonPulse { 0%,100%{opacity:1}50%{opacity:.4} }`}</style>
      <header style={{ marginBottom: '32px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}><Activity color="#3b82f6" /> Salud del Sistema</h1>
        <p style={{ margin: '8px 0 0 0', color: '#6b7280' }}>Monitorización en tiempo real de los servicios y dependencias (Spring Actuator).</p>
      </header>

      {error ? (
        <div style={{ background: '#fee2e2', color: '#991b1b', padding: '16px', borderRadius: '8px', fontWeight: '500' }}>⚠️ Error conectando con el backend de Actuator en :8080. Servidor apagado o inaccesible.</div>
      ) : !health ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '24px' }}>
          {[0, 1, 2, 3].map(i => <SkeletonCard key={i} delay={i * 0.1} />)}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(350px, 1fr))', gap: '24px' }}>
          <ComponentCard name="Backend Principal (SkyFence API)" icon={Activity} status={health.status} statusDetails={{ status: health.status }} />
          <ComponentCard name="Base de Datos (PostgreSQL)" icon={Database} status={health.components?.db?.status} statusDetails={health.components?.db?.details || {}} />
          <ComponentCard name="adsb.fi API" icon={Cloud} status={health.components?.adsbfi?.status} statusDetails={health.components?.adsbfi?.details || {}} />
          <ComponentCard name="Broker STOMP (WebSockets)" icon={Radio} status={health.components?.websocket?.status} statusDetails={health.components?.websocket?.details || {}} />
        </div>
      )}

      {GRAFANA_URL && (
        <section style={{ marginTop: '48px' }}>
          <header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px', flexWrap: 'wrap', gap: '12px' }}>
            <div>
              <h2 style={{ margin: 0, fontSize: '22px', color: '#111827', display: 'flex', alignItems: 'center', gap: '10px' }}>
                <BarChart3 color="#f59e0b" /> Métricas en vivo (Grafana)
              </h2>
              <p style={{ margin: '6px 0 0 0', color: '#6b7280' }}>Tráfico HTTP, JVM, base de datos y sistema — Prometheus + Grafana.</p>
            </div>
            <a
              href={GRAFANA_DASHBOARD}
              target="_blank"
              rel="noopener noreferrer"
              style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', background: '#111827', color: 'white', padding: '10px 16px', borderRadius: '8px', textDecoration: 'none', fontSize: '14px', fontWeight: '500' }}
            >
              Abrir en Grafana <ExternalLink size={16} />
            </a>
          </header>
          <iframe
            src={`${GRAFANA_DASHBOARD}?kiosk&refresh=30s&from=now-1h&to=now`}
            title="Dashboard SkyFence en Grafana"
            style={{ width: '100%', height: '900px', border: 'none', borderRadius: '12px', boxShadow: '0 4px 6px -1px rgba(0,0,0,0.1)', background: '#111217' }}
          />
        </section>
      )}
    </div>
  );
}
