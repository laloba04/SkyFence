import { useState, useEffect } from 'react';
import axios from 'axios';
import DroneMap from './components/DroneMap';
import { useWebSocket } from './hooks/useWebSocket';

export default function App() {
  const [aircraft, setAircraft] = useState([]);
  const [zones, setZones] = useState([]);
  const { alerts, connected } = useWebSocket();
  const alertIcaos = new Set(alerts.map(a => a.aircraftIcao));

  useEffect(() => {
    axios.get('http://localhost:8080/api/zones').then(r => setZones(r.data));
    const fetch = () => axios.get('http://localhost:8080/api/aircraft/live')
                              .then(r => setAircraft(r.data));
    fetch();
    const interval = setInterval(fetch, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial' }}>
      <div style={{ display:'flex', justifyContent:'space-between', marginBottom:'12px' }}>
        <h1>🚁 DroneTrack</h1>
        <span style={{ color: connected ? 'green' : 'red', fontSize: '13px' }}>
          {connected ? '● Conectado en tiempo real' : '○ Desconectado'}
        </span>
      </div>
      <DroneMap aircraft={aircraft} zones={zones} alertIcaos={alertIcaos} />
      {alerts.length > 0 && (
        <div style={{ marginTop: '20px' }}>
          <h3 style={{ color: '#cc0000' }}>Alertas activas ({alerts.length})</h3>
          {alerts.slice(0, 5).map((a, i) => (
            <div key={i} style={{ background:'#fff0f0', border:'1px solid #ffcccc',
                borderRadius:'6px', padding:'10px', marginBottom:'8px' }}>
              <strong>{a.aircraftCallsign || a.aircraftIcao}</strong> — {a.zoneName}
              <span style={{ marginLeft:'8px', color: a.severity==='HIGH' ? 'red':'orange',
                  fontSize:'12px', fontWeight:'bold' }}>{a.severity}</span>
              <div style={{ fontSize:'12px', color:'#666', marginTop:'4px' }}>
                {a.distanceKm} km · {new Date(a.detectedAt).toLocaleTimeString()}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
