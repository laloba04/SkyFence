export default function AlertPanel({ alerts }) {
  if (alerts.length === 0) return null;

  return (
    <div style={{ background: 'white', padding: '20px', borderRadius: '8px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
      <h3 style={{ margin: '0 0 16px 0', color: '#ef4444', fontSize: '16px' }}>
        Alertas ({alerts.length})
      </h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '260px', overflowY: 'auto' }}>
        {alerts.slice(0, 5).map((a, i) => (
          <div key={i} style={{ background: '#fef2f2', borderLeft: `4px solid ${(a.severity || a.severidad) === 'HIGH' ? '#dc2626' : '#f97316'}`, borderRadius: '4px', padding: '12px', fontSize: '13px', color: '#7f1d1d' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span><strong>{a.callsign || a.aircraftCallsign || a.icao24}</strong> en <strong>{a.zoneName}</strong></span>
              <span style={{ fontSize: '11px', fontWeight: 'bold', color: (a.severity || a.severidad) === 'HIGH' ? '#dc2626' : '#f97316', background: (a.severity || a.severidad) === 'HIGH' ? '#fee2e2' : '#fff7ed', padding: '2px 6px', borderRadius: '4px' }}>
                {a.severity || a.severidad || 'MEDIUM'}
              </span>
            </div>
            <div style={{ fontSize: '11px', color: '#ef4444', marginTop: '6px', opacity: 0.8 }}>
              {a.distance || a.distanceKm} km del centro · {new Date(a.detectedAt || Date.now()).toLocaleTimeString()}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
