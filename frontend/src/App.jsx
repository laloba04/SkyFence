import { useState, useEffect } from 'react';
import axios from 'axios';
import DroneMap from './components/DroneMap';
import ConfirmModal from './components/ConfirmModal';
import AlertPanel from './components/AlertPanel';
import ZoneManagerModal from './components/ZoneManagerModal';
import { useWebSocket } from './hooks/useWebSocket';

export default function App() {
  const [aircraft, setAircraft] = useState([]);
  const [zones, setZones] = useState([]);
  const [fetchError, setFetchError] = useState(null);
  const [lastUpdate, setLastUpdate] = useState('--');
  const [clearedAt, setClearedAt] = useState(null);
  const [mockAlerts, setMockAlerts] = useState([]);
  const [zoneFilter, setZoneFilter] = useState('ALL');
  const [isZoneModalOpen, setIsZoneModalOpen] = useState(false);
  const [zoneToDelete, setZoneToDelete] = useState(null);

  const { alerts: rawAlerts, connected } = useWebSocket();
  
  // Combinamos alertas reales de WebSocket con las alertas simuladas por el botón
  const allAlerts = [...mockAlerts, ...rawAlerts];
  const alerts = clearedAt ? allAlerts.filter(a => new Date(a.detectedAt || Date.now()) > clearedAt) : allAlerts;
  const alertIcaos = new Set(alerts.map(a => a.icao24 || a.aircraftIcao));
  // Mapa icao -> tipo de zona para colorear el icono según amenaza
  const alertZoneTypes = new Map(alerts.map(a => [a.icao24 || a.aircraftIcao, a.zoneType || a.type || 'AIRPORT']));
  const zoneTypes = ['ALL', ...new Set(zones.map(z => z.type))];
  const filteredZones = zoneFilter === 'ALL' ? zones : zones.filter(z => z.type === zoneFilter);

  useEffect(() => {
    axios.get('http://localhost:8080/api/zones')
      .then(r => setZones(Array.isArray(r.data) ? r.data : []))
      .catch(e => console.error('Error al cargar zonas:', e));

    const fetch = () => axios.get('http://localhost:8080/api/aircraft/live')
                              .then(r => { 
                                if(Array.isArray(r.data) && r.data.length > 0) {
                                  setAircraft(prev => {
                                    const mocks = prev.filter(a => a.icao24.startsWith('intruder'));
                                    return [...r.data, ...mocks]; // Mantiene los aviones simulados aunque lleguen nuevos
                                  });
                                } else {
                                  // Si la API responde bien pero sin aviones, cargamos los de prueba
                                  setAircraft([
                                    { icao24: 'sim1', callsign: 'SIM-MAD', latitude: 40.48, longitude: -3.50, altitude: 3000, velocity: 150 },
                                    { icao24: 'sim2', callsign: 'SIM-BCN', latitude: 41.35, longitude: 2.15, altitude: 4000, velocity: 180 },
                                    { icao24: 'sim3', callsign: 'SIM-VLC', latitude: 39.48, longitude: -0.48, altitude: 2500, velocity: 120 }
                                  ]);
                                }
                                setFetchError(null);
                                setLastUpdate(new Date().toLocaleTimeString());
                              })
                              .catch(e => {
                                console.error('Error al obtener datos de aeronaves:', e);
                                setFetchError('Límite de OpenSky o backend inactivo. Mostrando aeronaves de prueba.');
                                // 🚁 Inyectamos aviones simulados para no bloquear el diseño
                                setAircraft([
                                  { icao24: 'sim1', callsign: 'SIM-MAD', latitude: 40.48, longitude: -3.50, altitude: 3000, velocity: 150 },
                                  { icao24: 'sim2', callsign: 'SIM-BCN', latitude: 41.35, longitude: 2.15, altitude: 4000, velocity: 180 },
                                  { icao24: 'sim3', callsign: 'SIM-VLC', latitude: 39.48, longitude: -0.48, altitude: 2500, velocity: 120 }
                                ]);
                              });
    fetch();
    const interval = setInterval(fetch, 15000); // Subimos a 15s para no saturar OpenSky
    return () => clearInterval(interval);
  }, []);

  const handleClearAlerts = () => setClearedAt(new Date());

  const executeDeleteZone = async () => {
    if (!zoneToDelete) return;
    try {
      await axios.delete(`http://localhost:8080/api/zones/${zoneToDelete.id}`);
      setZones(prev => prev.filter(z => z.id !== zoneToDelete.id));
      setZoneToDelete(null);
    } catch (e) {
      alert("Error al eliminar la zona.");
    }
  };

  const handleZoneAdded = (newZone) => {
    setZones(prev => [...prev, newZone]);
  };

  // Acción para el botón de simular intrusión
  const handleSimulateIntrusion = () => {
    // Generamos un ID aleatorio entre 1000 y 9999 para que cada dron sea único
    const randomId = Math.floor(Math.random() * 9000) + 1000;
    const newIcao = `intruder-${randomId}`;
    const newCallsign = `UAV-${randomId}`;

    // Le damos una posición ligeramente aleatoria alrededor de Barajas
    const randomLat = 40.498 + (Math.random() * 0.04 - 0.02);
    const randomLon = -3.567 + (Math.random() * 0.04 - 0.02);

    const mockIntruder = { icao24: newIcao, callsign: newCallsign, latitude: randomLat, longitude: randomLon, altitude: 120, velocity: 45 };
    
    const mockAlert = {
      icao24: newIcao, 
      callsign: newCallsign, 
      zoneName: 'Aeropuerto Madrid-Barajas (Simulación)',
      zoneType: 'AIRPORT',
      distance: (Math.random() * 4.9 + 0.1).toFixed(2), // Distancia aleatoria entre 0.1 y 5.0 km
      severity: 'HIGH',
      detectedAt: new Date().toISOString()
    };
    
    setAircraft(prev => [...prev, mockIntruder]); // Añadimos el nuevo dron sin borrar los anteriores
    setMockAlerts(prev => [mockAlert, ...prev]);
    setClearedAt(null); // Reseteamos el filtro de limpiar para que se vea la alerta
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'system-ui, -apple-system, sans-serif', background: '#f5f7fa', minHeight: '100vh' }}>
      
      {/* Cabecera superior */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems: 'center', marginBottom:'20px', background: 'white', padding: '15px 20px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
        <h1 style={{ margin: 0, fontSize: '24px', color: '#1f2937' }}>✈ SkyFence</h1>
        <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
          <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer" style={{ textDecoration: 'none', color: '#3b82f6', fontWeight: '600', fontSize: '14px' }}>Swagger API ↗</a>
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
        <div style={{ background: '#fef2f2', color: '#ef4444', padding: '12px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', border: '1px solid #fecaca', fontWeight: '500' }}>
          ⚠️ {fetchError}
        </div>
      )}

      {/* Contenido Principal a dos columnas */}
      <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
         
         {/* Columna Izquierda: Mapa */}
         <div style={{ flex: '3', minWidth: '600px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
               <h2 style={{ margin: 0, fontSize: '18px', color: '#374151' }}>Mapa en tiempo real</h2>
               <div style={{ display: 'flex', gap: '10px' }}>
                  <button onClick={handleSimulateIntrusion} style={{...btnStyle, background: '#3b82f6', color: 'white'}}>Simular intrusión ↑</button>
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
                  {zones.length === 0 ? <li style={{ fontSize: '13px', color: '#9ca3af' }}>Cargando zonas...</li> :
                   filteredZones.map(z => {
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

// --- Componentes auxiliares de diseño ---

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
