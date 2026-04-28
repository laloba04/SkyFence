import { useState, useEffect, useRef } from 'react';
import axios from 'axios';
import { Toaster, toast } from 'react-hot-toast';
import DroneMap from '../components/DroneMap';
import ConfirmModal from '../components/ConfirmModal';
import AlertPanel from '../components/AlertPanel';
import ZoneManagerModal from '../components/ZoneManagerModal';
import { useWebSocket } from '../hooks/useWebSocket';

const SIM_AIRCRAFT = [
  { icao24: 'sim1', callsign: 'SIM-MAD', latitude: 40.48, longitude: -3.50, altitude: 3000, velocity: 150 },
  { icao24: 'sim2', callsign: 'SIM-BCN', latitude: 41.35, longitude: 2.15, altitude: 4000, velocity: 180 },
  { icao24: 'sim3', callsign: 'SIM-VLC', latitude: 39.48, longitude: -0.48, altitude: 2500, velocity: 120 }
];

export default function Dashboard() {
  const [aircraft, setAircraft] = useState([]);
  const [zones, setZones] = useState([]);
  const [zonesLoading, setZonesLoading] = useState(true);
  const [fetchError, setFetchError] = useState(null);
  const [retryCountdown, setRetryCountdown] = useState(null);
  const retryTimerRef = useRef(null);
  const [lastUpdate, setLastUpdate] = useState('--');
  const [clearedAt, setClearedAt] = useState(null);
  const [mockAlerts, setMockAlerts] = useState([]);
  const [zoneFilter, setZoneFilter] = useState('ALL');
  const [isZoneModalOpen, setIsZoneModalOpen] = useState(false);
  const [zoneToDelete, setZoneToDelete] = useState(null);
  const [simZoneId, setSimZoneId] = useState('');
  const [simulating, setSimulating] = useState(false);

  const { alerts: rawAlerts, connected } = useWebSocket();

  const allAlerts = [...mockAlerts, ...rawAlerts];
  const alerts = clearedAt ? allAlerts.filter(a => new Date(a.detectedAt || Date.now()) > clearedAt) : allAlerts;
  const alertIcaos = new Set(alerts.map(a => a.icao24 || a.aircraftIcao));
  const alertZoneTypes = new Map(alerts.map(a => [a.icao24 || a.aircraftIcao, a.zoneType || a.type || 'AIRPORT']));
  const zoneTypes = ['ALL', ...new Set(zones.map(z => z.type))];
  const filteredZones = zoneFilter === 'ALL' ? zones : zones.filter(z => z.type === zoneFilter);

  const fetchAircraftData = () =>
    axios.get(`${import.meta.env.VITE_API_URL}/api/aircraft/live`)
      .then(r => {
        if (Array.isArray(r.data) && r.data.length > 0) {
          setAircraft(prev => {
            const mocks = prev.filter(a => a.icao24.startsWith('intruder'));
            return [...r.data, ...mocks];
          });
        } else {
          setAircraft(SIM_AIRCRAFT);
        }
        setFetchError(null);
        setLastUpdate(new Date().toLocaleTimeString());
      })
      .catch(e => {
        console.error('Error al obtener datos de aeronaves:', e);
        setFetchError('La API no está disponible. Mostrando aeronaves de prueba.');
        setAircraft(SIM_AIRCRAFT);
      });

  useEffect(() => {
    axios.get(`${import.meta.env.VITE_API_URL}/api/zones`)
      .then(r => { setZones(Array.isArray(r.data) ? r.data : []); setZonesLoading(false); })
      .catch(() => setZonesLoading(false));

    fetchAircraftData();
    const interval = setInterval(fetchAircraftData, 15000);
    return () => clearInterval(interval);
  }, []);

  // Start/stop countdown + auto-retry when error state changes
  useEffect(() => {
    clearInterval(retryTimerRef.current);
    if (!fetchError) { setRetryCountdown(null); return; }
    setRetryCountdown(30);
    retryTimerRef.current = setInterval(() => {
      setRetryCountdown(prev => {
        if (prev <= 1) { fetchAircraftData(); return 30; }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(retryTimerRef.current);
  }, [fetchError]);

  const handleRetry = () => {
    clearInterval(retryTimerRef.current);
    setRetryCountdown(30);
    fetchAircraftData();
    retryTimerRef.current = setInterval(() => {
      setRetryCountdown(prev => {
        if (prev <= 1) { fetchAircraftData(); return 30; }
        return prev - 1;
      });
    }, 1000);
  };

  const handleClearAlerts = () => setClearedAt(new Date());

  const executeDeleteZone = async () => {
    if (!zoneToDelete) return;
    try {
      await axios.delete(`${import.meta.env.VITE_API_URL}/api/zones/${zoneToDelete.id}`);
      setZones(prev => prev.filter(z => z.id !== zoneToDelete.id));
      toast.success('Zona eliminada correctamente');
      setZoneToDelete(null);
    } catch (e) {
      toast.error("Error intermitente al eliminar la zona.");
    }
  };

  const handleZoneAdded = (newZone) => {
    setZones(prev => [...prev, newZone]);
    toast.success(`Zona "${newZone.name}" creada con éxito`);
  };

  const handleSimulateIntrusion = async () => {
    if (!simZoneId) { toast.error('Selecciona una zona primero'); return; }
    setSimulating(true);
    try {
      const res = await axios.post(
        `${import.meta.env.VITE_API_URL}/api/simulate?zoneId=${simZoneId}`
      );
      const alert = res.data;
      const zone = zones.find(z => z.id === Number(simZoneId));
      if (zone) {
        const fraction = 0.25;
        const fakeLat = zone.latitude + (zone.radiusKm * fraction / 111.0);
        const fakeLon = zone.longitude;
        setAircraft(prev => [...prev, {
          icao24: alert.aircraftIcao,
          callsign: alert.aircraftCallsign,
          latitude: fakeLat,
          longitude: fakeLon,
          altitude: 120,
          velocity: 45
        }]);
      }
      setClearedAt(null);
      toast.success(`Intrusión simulada en "${alert.zoneName}"`);
    } catch {
      toast.error('Error al simular intrusión');
    } finally {
      setSimulating(false);
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'system-ui, -apple-system, sans-serif' }}>
      <style>{`@keyframes skeletonPulse { 0%,100%{opacity:1}50%{opacity:.4} }`}</style>
      <Toaster position="bottom-right" />

      {/* Cabecera superior */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems: 'center', marginBottom:'20px', background: 'white', padding: '15px 20px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
        <h2 style={{ margin: 0, fontSize: '20px', color: '#1f2937' }}>Visor Geográfico</h2>
        <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <a href={`${import.meta.env.VITE_API_URL}/swagger-ui/index.html`} target="_blank" rel="noreferrer" style={{ textDecoration: 'none', color: '#3b82f6', fontWeight: '600', fontSize: '14px' }}>Swagger API ↗</a>
          <span style={{ color: connected ? '#10b981' : '#ef4444', fontSize: '14px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>{connected ? '🟢' : '🔴'}</span> {connected ? 'conectado' : 'desconectado'}
          </span>
        </div>
      </div>

      {/* Fila de Estadísticas */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '15px', marginBottom: '20px' }}>
         <StatCard title="alertas" value={alerts.length} color={alerts.length > 0 ? '#ef4444' : '#1f2937'} />
         <StatCard title="en vuelo" value={aircraft.length} />
         <StatCard title="zonas vigiladas" value={zones.length} />
         <StatCard title="aeronaves en zona" value={alertIcaos.size} color={alertIcaos.size > 0 ? '#f97316' : '#1f2937'} />
         <StatCard title="actualización" value={lastUpdate} />
      </div>

      {fetchError && (
        <div style={{ background: '#fef2f2', color: '#b91c1c', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', border: '1px solid #fecaca', fontWeight: '500', display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px' }}>
          <span>⚠️ {fetchError}{retryCountdown !== null ? ` Recargando en ${retryCountdown}s…` : ''}</span>
          <button
            onClick={handleRetry}
            style={{ background: '#ef4444', color: 'white', border: 'none', borderRadius: '6px', padding: '6px 14px', fontSize: '13px', fontWeight: '600', cursor: 'pointer', flexShrink: 0, transition: 'opacity 0.2s' }}>
            Reintentar
          </button>
        </div>
      )}

      {/* Contenido Principal a dos columnas */}
      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>

         {/* Columna Izquierda: Mapa */}
         <div style={{ flex: '3', minWidth: '600px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
               <h2 style={{ margin: 0, fontSize: '18px', color: '#374151' }}>Mapa en tiempo real</h2>
               <div style={{ display: 'flex', gap: '10px' }}>
                  <select value={simZoneId} onChange={e => setSimZoneId(e.target.value)}
                    style={{ padding: '8px 10px', borderRadius: '6px', border: '1px solid #d1d5db', fontSize: '13px', background: 'white', cursor: 'pointer', maxWidth: '180px' }}>
                    <option value="">Zona a simular…</option>
                    {zones.map(z => <option key={z.id} value={z.id}>{z.name}</option>)}
                  </select>
                  <button onClick={handleSimulateIntrusion} disabled={simulating || !simZoneId}
                    style={{...btnStyle, background: simZoneId ? '#3b82f6' : '#9ca3af', color: 'white', opacity: simulating ? 0.7 : 1}}>
                    {simulating ? 'Simulando…' : 'Simular intrusión'}
                  </button>
                  <button onClick={handleClearAlerts} style={{...btnStyle, background: '#e5e7eb', color: '#374151'}}>Limpiar alertas</button>
               </div>
            </div>

            <div style={{ background: 'white', padding: '12px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
               <DroneMap aircraft={aircraft} zones={filteredZones} alertIcaos={alertIcaos} alertZoneTypes={alertZoneTypes} />

               {/* Leyenda */}
               <div style={{ display: 'flex', gap: '20px', marginTop: '16px', fontSize: '13px', color: '#4b5563', justifyContent: 'center', fontWeight: '500', flexWrap: 'wrap' }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ color: '#4b5563' }}>✈</span> aeronave normal</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ color: '#eab308' }}>✈</span> intrusión detectada</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ color: '#3b82f6', fontSize: '16px' }}>○</span> AIRPORT</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ color: '#dc2626', fontSize: '16px' }}>○</span> MILITARY</span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}><span style={{ color: '#f97316', fontSize: '16px' }}>○</span> NUCLEAR</span>
               </div>
            </div>
         </div>

         {/* Columna Derecha: Zonas y Alertas */}
         <div style={{ flex: '1', minWidth: '280px', display: 'flex', flexDirection: 'column', gap: '20px' }}>

            <div style={{ background: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
               <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                 <h3 style={{ margin: 0, fontSize: '16px', color: '#374151', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    Zonas vigiladas
                    <button onClick={() => setIsZoneModalOpen(true)} style={{ background: '#e0e7ff', color: '#4338ca', border: 'none', borderRadius: '4px', padding: '2px 8px', fontSize: '12px', cursor: 'pointer', fontWeight: 'bold', transition: 'background 0.2s' }}>+ Añadir</button>
                 </h3>
                 <select value={zoneFilter} onChange={e => setZoneFilter(e.target.value)} style={{ fontSize: '12px', background: '#f3f4f6', padding: '2px 8px', borderRadius: '12px', color: '#4b5563', border: 'none', cursor: 'pointer', outline: 'none' }}>
                   {zoneTypes.map(t => <option key={t} value={t}>{t === 'ALL' ? 'Todas las zonas' : t}</option>)}
                 </select>
               </div>
               <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '260px', overflowY: 'auto' }}>
                  {zonesLoading
                    ? [0, 1, 2].map(i => (
                        <li key={i} style={{ height: '28px', background: '#f3f4f6', borderRadius: '6px', animation: 'skeletonPulse 1.5s ease-in-out infinite', animationDelay: `${i * 0.15}s` }} />
                      ))
                    : zones.length === 0
                    ? <li style={{ fontSize: '13px', color: '#9ca3af' }}>No hay zonas configuradas.</li>
                    : filteredZones.map(z => {
                        const badge = z.type === 'AIRPORT'
                          ? { bg: '#e0f2fe', color: '#1e40af' }
                          : z.type === 'NUCLEAR'
                          ? { bg: '#fff7ed', color: '#c2410c' }
                          : { bg: '#fee2e2', color: '#991b1b' };
                        return (
                          <li key={z.id} style={{ display: 'flex', alignItems: 'center', fontSize: '14px', borderBottom: '1px solid #f3f4f6', paddingBottom: '10px', color: '#4b5563' }}>
                            <span style={{ flex: 1, textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>📍 <strong>{z.name}</strong></span>
                            <span style={{ fontSize: '11px', background: badge.bg, color: badge.color, padding: '2px 6px', borderRadius: '4px', marginRight: '6px' }}>{z.type}</span>
                            <button onClick={() => setZoneToDelete(z)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#ef4444', fontSize: '14px', padding: '0 4px', transition: 'transform 0.1s' }} title="Eliminar zona" onMouseOver={(e) => e.target.style.transform='scale(1.2)'} onMouseOut={(e) => e.target.style.transform='scale(1)'}>🗑️</button>
                          </li>
                        );
                      })
                  }
               </ul>
            </div>

            <AlertPanel alerts={alerts} />
         </div>
      </div>

      <ZoneManagerModal
        isOpen={isZoneModalOpen}
        onClose={() => setIsZoneModalOpen(false)}
        onZoneAdded={handleZoneAdded}
      />

      <ConfirmModal
        isOpen={zoneToDelete !== null}
        title="Eliminar Zona Restringida"
        message={`¿Estás seguro de que quieres eliminar la zona "${zoneToDelete?.name}"? Esta acción no se puede deshacer y las alertas asociadas seguirán existiendo en el histórico, pero la vigilancia de coordenadas cesará de inmediato.`}
        onCancel={() => setZoneToDelete(null)}
        onConfirm={executeDeleteZone}
      />
    </div>
  );
}

function StatCard({ title, value, color = '#3b82f6' }) {
  return (
    <div style={{ background: 'white', padding: '16px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
      <span style={{ fontSize: '11px', color: '#6b7280', textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: '8px', fontWeight: '600' }}>{title}</span>
      <span style={{ fontSize: '28px', fontWeight: 'bold', color: color }}>{value}</span>
    </div>
  );
}

const btnStyle = {
  padding: '8px 14px',
  border: 'none',
  borderRadius: '6px',
  cursor: 'pointer',
  fontSize: '13px',
  fontWeight: '600',
  transition: 'opacity 0.2s'
};
