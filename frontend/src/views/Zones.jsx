import { useState, useEffect } from 'react';
import axios from 'axios';
import { MapPin, Plus, Trash2, AlertTriangle, RefreshCw } from 'lucide-react';
import { authHeader } from '../auth';
import ZoneManagerModal from '../components/ZoneManagerModal';

const API = import.meta.env.VITE_API_URL;

const TYPE_STYLES = {
  AIRPORT:  { bg: '#eff6ff', color: '#1d4ed8', label: 'Aeropuerto' },
  MILITARY: { bg: '#fef2f2', color: '#b91c1c', label: 'Base Militar' },
  NUCLEAR:  { bg: '#fffbeb', color: '#b45309', label: 'C. Nuclear' },
};

export default function Zones() {
  const [zones, setZones]     = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError]     = useState(null);
  const [deleting, setDeleting] = useState(null);
  const [modalOpen, setModalOpen] = useState(false);

  const fetchZones = () => {
    setLoading(true);
    setError(null);
    axios.get(`${API}/api/zones`)
      .then(r => setZones(r.data))
      .catch(() => setError('No se pudo cargar la lista de zonas.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchZones(); }, []);

  const handleDelete = async (zone) => {
    if (!window.confirm(`¿Eliminar la zona "${zone.name}"? Esta acción no se puede deshacer.`)) return;
    setDeleting(zone.id);
    try {
      await axios.delete(`${API}/api/zones/${zone.id}`, { headers: authHeader() });
      setZones(prev => prev.filter(z => z.id !== zone.id));
    } catch {
      setError('Error al eliminar la zona.');
    } finally {
      setDeleting(null);
    }
  };

  const handleZoneAdded = (newZone) => {
    setZones(prev => [...prev, newZone]);
  };

  return (
    <div style={{ padding: '32px', maxWidth: '1000px', margin: '0 auto' }}>
      <header style={{ marginBottom: '28px', display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: '28px', color: '#111827', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <MapPin color="#3b82f6" /> Zonas Restringidas
          </h1>
          <p style={{ margin: '6px 0 0 0', color: '#6b7280' }}>
            Gestión de zonas del espacio aéreo protegido.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button onClick={fetchZones} disabled={loading}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 14px', borderRadius: '8px',
                     background: 'white', color: '#374151', border: '1px solid #d1d5db', cursor: 'pointer', fontWeight: 500, fontSize: '14px' }}>
            <RefreshCw size={15} style={{ animation: loading ? 'spin 1s linear infinite' : 'none' }} />
            Actualizar
          </button>
          <button onClick={() => setModalOpen(true)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px',
                     background: '#3b82f6', color: 'white', border: 'none', cursor: 'pointer', fontWeight: 500, fontSize: '14px' }}>
            <Plus size={15} /> Añadir Zona
          </button>
        </div>
      </header>

      {error && (
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '14px 16px',
                      background: '#fef2f2', border: '1px solid #fca5a5', borderRadius: '10px',
                      color: '#991b1b', marginBottom: '20px' }}>
          <AlertTriangle size={18} /> {error}
        </div>
      )}

      {loading ? (
        <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          {[0, 1, 2].map(i => (
            <div key={i} style={{ padding: '16px', borderBottom: '1px solid #f3f4f6', display: 'flex', gap: '16px', alignItems: 'center' }}>
              <div style={{ width: 160, height: 18, background: '#f3f4f6', borderRadius: 4, animation: 'skeletonPulse 1.5s ease-in-out infinite', animationDelay: `${i * 0.15}s` }} />
              <div style={{ width: 80, height: 22, background: '#f3f4f6', borderRadius: 9999, animation: 'skeletonPulse 1.5s ease-in-out infinite', animationDelay: `${i * 0.15 + 0.05}s` }} />
            </div>
          ))}
        </div>
      ) : !error && zones.length === 0 ? (
        <div style={{ background: 'white', padding: '56px 40px', borderRadius: '12px',
                      border: '1px dashed #d1d5db', textAlign: 'center', color: '#6b7280' }}>
          <MapPin size={44} color="#d1d5db" style={{ marginBottom: '14px' }} />
          <h2 style={{ margin: '0 0 8px', color: '#374151' }}>Sin zonas registradas</h2>
          <p style={{ margin: '0 0 20px' }}>Añade la primera zona restringida del sistema.</p>
          <button onClick={() => setModalOpen(true)}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '10px 18px', borderRadius: '8px',
                     background: '#3b82f6', color: 'white', border: 'none', cursor: 'pointer', fontWeight: 500 }}>
            <Plus size={15} /> Añadir Zona
          </button>
        </div>
      ) : (
        <div style={{ background: 'white', borderRadius: '12px', border: '1px solid #e5e7eb', overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ background: '#f9fafb', borderBottom: '1px solid #e5e7eb' }}>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>ID</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Nombre</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Tipo</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Coordenadas</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Radio</th>
                <th style={{ padding: '12px 16px', textAlign: 'left', fontWeight: 600, color: '#374151' }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {zones.map((z, i) => {
                const ts = TYPE_STYLES[z.type] || { bg: '#f3f4f6', color: '#374151', label: z.type };
                return (
                  <tr key={z.id} style={{ borderBottom: '1px solid #f3f4f6', background: i % 2 === 0 ? 'white' : '#fafafa' }}>
                    <td style={{ padding: '12px 16px', color: '#9ca3af', fontFamily: 'monospace' }}>#{z.id}</td>
                    <td style={{ padding: '12px 16px', color: '#111827', fontWeight: 500 }}>{z.name}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <span style={{ display: 'inline-block', padding: '3px 10px', borderRadius: 9999,
                                     fontSize: 12, fontWeight: 600, background: ts.bg, color: ts.color }}>
                        {ts.label}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', color: '#4b5563', fontFamily: 'monospace', fontSize: 13 }}>
                      {z.latitude?.toFixed(4)}, {z.longitude?.toFixed(4)}
                    </td>
                    <td style={{ padding: '12px 16px', color: '#4b5563' }}>{z.radiusKm} km</td>
                    <td style={{ padding: '12px 16px' }}>
                      <button
                        onClick={() => handleDelete(z)}
                        disabled={deleting === z.id}
                        title="Eliminar zona"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: 5,
                                 padding: '6px 12px', borderRadius: 8, fontSize: 13,
                                 border: '1px solid #fca5a5', background: '#fef2f2',
                                 color: '#dc2626', cursor: 'pointer', fontWeight: 500,
                                 opacity: deleting === z.id ? 0.6 : 1 }}>
                        <Trash2 size={13} />
                        {deleting === z.id ? 'Eliminando…' : 'Eliminar'}
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      <ZoneManagerModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onZoneAdded={handleZoneAdded}
      />

      <style>{`
        @keyframes spin { to { transform: rotate(360deg); } }
        @keyframes skeletonPulse { 0%,100%{opacity:1}50%{opacity:.4} }
      `}</style>
    </div>
  );
}
