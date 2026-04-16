import { useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import { Bell, ChevronLeft, ChevronRight, Filter, RefreshCw, AlertTriangle, Info } from 'lucide-react';

const API = import.meta.env.VITE_API_URL;

const SEVERITY_COLORS = {
  HIGH:   { bg: '#fef2f2', text: '#991b1b', border: '#fca5a5', dot: '#ef4444' },
  MEDIUM: { bg: '#fffbeb', text: '#92400e', border: '#fcd34d', dot: '#f59e0b' },
};

const ZONE_TYPE_LABELS = {
  AIRPORT:  'Aeropuerto',
  MILITARY: 'Militar',
  NUCLEAR:  'Nuclear',
};

function SeverityBadge({ severity }) {
  const c = SEVERITY_COLORS[severity] || { bg: '#f3f4f6', text: '#374151', border: '#d1d5db', dot: '#9ca3af' };
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: '6px',
      padding: '3px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 600,
      background: c.bg, color: c.text, border: `1px solid ${c.border}`
    }}>
      <span style={{ width: 7, height: 7, borderRadius: '50%', background: c.dot, flexShrink: 0 }} />
      {severity}
    </span>
  );
}

function ZoneTypeBadge({ type }) {
  const icons = { AIRPORT: '✈', MILITARY: '⚔', NUCLEAR: '☢' };
  return (
    <span style={{ fontSize: '13px', color: '#6b7280' }}>
      {icons[type] || '📍'} {ZONE_TYPE_LABELS[type] || type}
    </span>
  );
}

export default function Alerts() {
  const [data, setData]         = useState(null);   // Page<Alert> from API
  const [page, setPage]         = useState(0);
  const [size]                  = useState(20);
  const [severity, setSeverity] = useState('');
  const [hours, setHours]       = useState('');
  const [loading, setLoading]   = useState(false);
  const [error, setError]       = useState(null);

  const fetchAlerts = useCallback(() => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ page, size });
    if (severity) params.set('severity', severity);
    if (hours)    params.set('hours', hours);

    axios.get(`${API}/api/alerts?${params}`)
      .then(r => setData(r.data))
      .catch(() => setError('No se pudo cargar el histórico de alertas.'))
      .finally(() => setLoading(false));
  }, [page, size, severity, hours]);

  useEffect(() => { fetchAlerts(); }, [fetchAlerts]);

  const totalPages = data?.totalPages ?? 0;
  const alerts     = data?.content ?? [];
  const total      = data?.totalElements ?? 0;

  function handleFilterChange(newSeverity, newHours) {
    setSeverity(newSeverity);
    setHours(newHours);
    setPage(0);
  }

  return (
    <div style={{ padding: '32px', maxWidth: '1200px', margin: '0 auto' }}>
      {/* Header */}
      <header style={{ marginBottom: '28px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Bell color="#3b82f6" /> Histórico de Alertas
          </h1>
          <p style={{ margin: '6px 0 0 0', color: '#6b7280' }}>
            Registro persistido de intrusiones detectadas en zonas restringidas.
          </p>
        </div>
        <button onClick={fetchAlerts} disabled={loading}
          style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px',
                   background: '#3b82f6', color: 'white', border: 'none', cursor: 'pointer', fontWeight: 500, fontSize: '14px' }}>
          <RefreshCw size={15} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
          Actualizar
        </button>
      </header>

      {/* Filters */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px', flexWrap: 'wrap', alignItems: 'center' }}>
        <Filter size={16} color="#6b7280" />
        <select value={severity} onChange={e => handleFilterChange(e.target.value, hours)}
          style={{ padding: '7px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', background: 'white', cursor: 'pointer' }}>
          <option value="">Todas las severidades</option>
          <option value="HIGH">HIGH</option>
          <option value="MEDIUM">MEDIUM</option>
        </select>

        <select value={hours} onChange={e => handleFilterChange(severity, e.target.value)}
          style={{ padding: '7px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', background: 'white', cursor: 'pointer' }}>
          <option value="">Todo el historial</option>
          <option value="1">Última hora</option>
          <option value="6">Últimas 6 horas</option>
          <option value="24">Últimas 24 horas</option>
          <option value="168">Última semana</option>
        </select>

        {total > 0 && (
          <span style={{ marginLeft: 'auto', fontSize: '13px', color: '#6b7280' }}>
            {total} alerta{total !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* Error */}
      {error && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '14px 16px',
                      background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: '10px',
                      color: '#991b1b', marginBottom: '20px' }}>
          <AlertTriangle size={18} /> {error}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && alerts.length === 0 && (
        <div style={{ background: 'white', padding: '56px 40px', borderRadius: '12px',
                      border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
          <Info size={44} color="#d1d5db" style={{ marginBottom: '14px' }} />
          <h2 style={{ margin: '0 0 8px', color: '#374151' }}>Sin alertas</h2>
          <p style={{ margin: 0 }}>
            {severity || hours
              ? 'No hay alertas con los filtros seleccionados.'
              : 'Todavía no se ha registrado ninguna intrusión.'}
          </p>
        </div>
      )}

      {/* Table */}
      {alerts.length > 0 && (
        <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                {['Aeronave', 'Callsign', 'Zona', 'Tipo', 'Distancia', 'Severidad', 'Detectada'].map(h => (
                  <th key={h} style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151', whiteSpace: 'nowrap' }}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {alerts.map((a, i) => (
                <tr key={a.id} style={{ borderBottom: '1px solid #f3f4f6', background: i % 2 === 0 ? 'white' : '#fafafa' }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', color: '#1d4ed8', fontWeight: 600 }}>
                    {a.aircraftIcao?.toUpperCase()}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#374151' }}>
                    {a.aircraftCallsign || 'N/A'}
                  </td>
                  <td style={{ padding: '12px 16px', color: '#111827', fontWeight: 500 }}>
                    {a.zoneName}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <ZoneTypeBadge type={a.zoneType} />
                  </td>
                  <td style={{ padding: '12px 16px', color: '#374151' }}>
                    {a.distanceKm != null ? `${a.distanceKm.toFixed(2)} km` : '—'}
                  </td>
                  <td style={{ padding: '12px 16px' }}>
                    <SeverityBadge severity={a.severity} />
                  </td>
                  <td style={{ padding: '12px 16px', color: '#6b7280', whiteSpace: 'nowrap' }}>
                    {new Date(a.detectedAt).toLocaleString('es-ES', {
                      day: '2-digit', month: '2-digit', year: 'numeric',
                      hour: '2-digit', minute: '2-digit', second: '2-digit'
                    })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px',
                          padding: '14px 16px', borderTop: '1px solid #f3f4f6' }}>
              <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                style={{ display: 'flex', alignItems: 'center', padding: '6px 12px', borderRadius: '8px',
                         border: '1px solid #d1d5db', background: 'white', cursor: page === 0 ? 'not-allowed' : 'pointer',
                         opacity: page === 0 ? 0.4 : 1 }}>
                <ChevronLeft size={16} />
              </button>

              {[...Array(totalPages)].map((_, i) => {
                if (totalPages <= 7 || Math.abs(i - page) <= 2 || i === 0 || i === totalPages - 1) {
                  return (
                    <button key={i} onClick={() => setPage(i)}
                      style={{ minWidth: '36px', padding: '6px', borderRadius: '8px', fontSize: '14px',
                               border: i === page ? 'none' : '1px solid #d1d5db',
                               background: i === page ? '#3b82f6' : 'white',
                               color: i === page ? 'white' : '#374151',
                               cursor: 'pointer', fontWeight: i === page ? 600 : 400 }}>
                      {i + 1}
                    </button>
                  );
                }
                if (Math.abs(i - page) === 3) return <span key={i} style={{ color: '#9ca3af' }}>…</span>;
                return null;
              })}

              <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                style={{ display: 'flex', alignItems: 'center', padding: '6px 12px', borderRadius: '8px',
                         border: '1px solid #d1d5db', background: 'white', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
                         opacity: page >= totalPages - 1 ? 0.4 : 1 }}>
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </div>
      )}

      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
