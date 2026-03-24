import { useState } from 'react';
import axios from 'axios';

export default function ZoneManagerModal({ isOpen, onClose, onZoneAdded }) {
  const [formData, setFormData] = useState({
    name: '',
    type: 'AIRPORT',
    latitude: '',
    longitude: '',
    radiusKm: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const payload = {
      name: formData.name,
      type: formData.type,
      latitude: parseFloat(formData.latitude),
      longitude: parseFloat(formData.longitude),
      radiusKm: parseFloat(formData.radiusKm)
    };

    if (isNaN(payload.latitude) || isNaN(payload.longitude) || isNaN(payload.radiusKm)) {
      setError("Por favor, introduce coordenadas y radio numéricos válidos.");
      setLoading(false);
      return;
    }

    try {
      const resp = await axios.post('http://localhost:8080/api/zones', payload);
      onZoneAdded(resp.data);
      setFormData({ name: '', type: 'AIRPORT', latitude: '', longitude: '', radiusKm: '' });
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Error al conectar con la API para crear la zona.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
      background: 'rgba(0,0,0,0.4)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 9999
    }}>
      <div style={{
        background: 'white', padding: '24px', borderRadius: '12px', width: '400px',
        boxShadow: '0 4px 6px rgba(0,0,0,0.1), 0 1px 3px rgba(0,0,0,0.08)',
        fontFamily: 'system-ui, -apple-system, sans-serif'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <h2 style={{ margin: 0, fontSize: '18px', color: '#1f2937' }}>Añadir Nueva Zona</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', fontSize: '20px', cursor: 'pointer', color: '#9ca3af' }}>✖</button>
        </div>

        {error && (
          <div style={{ background: '#fef2f2', color: '#ef4444', padding: '10px', borderRadius: '6px', fontSize: '13px', marginBottom: '16px' }}>
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div>
            <label style={labelStyle}>Nombre de la zona</label>
            <input name="name" value={formData.name} onChange={handleChange} required style={inputStyle} placeholder="ej: Aeropuerto Adolfo Suárez" />
          </div>

          <div>
            <label style={labelStyle}>Tipo de zona</label>
            <select name="type" value={formData.type} onChange={handleChange} required style={{...inputStyle, background: 'white'}}>
              <option value="AIRPORT">Aeropuerto (AIRPORT)</option>
              <option value="MILITARY">Base Militar (MILITARY)</option>
              <option value="NUCLEAR">Central Nuclear (NUCLEAR)</option>
            </select>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <div style={{ flex: 1 }}>
              <label style={labelStyle}>Latitud</label>
              <input name="latitude" type="number" step="any" value={formData.latitude} onChange={handleChange} required style={inputStyle} placeholder="40.4983" />
            </div>
            <div style={{ flex: 1 }}>
              <label style={labelStyle}>Longitud</label>
              <input name="longitude" type="number" step="any" value={formData.longitude} onChange={handleChange} required style={inputStyle} placeholder="-3.5676" />
            </div>
          </div>

          <div>
            <label style={labelStyle}>Radio de alcance (km)</label>
            <input name="radiusKm" type="number" step="0.1" value={formData.radiusKm} onChange={handleChange} required style={inputStyle} placeholder="5.0" />
          </div>

          <div style={{ display: 'flex', gap: '12px', marginTop: '10px' }}>
            <button type="button" onClick={onClose} style={{ ...btnStyle, background: '#f3f4f6', color: '#4b5563', flex: 1 }}>Cancelar</button>
            <button type="submit" disabled={loading} style={{ ...btnStyle, background: '#3b82f6', color: 'white', flex: 1 }}>
              {loading ? 'Guardando...' : 'Guardar Zona'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

const labelStyle = {
  display: 'block',
  fontSize: '12px',
  fontWeight: '600',
  color: '#4b5563',
  marginBottom: '4px',
  textTransform: 'uppercase',
  letterSpacing: '0.05em'
};

const inputStyle = {
  width: '100%',
  padding: '8px 12px',
  borderRadius: '6px',
  border: '1px solid #d1d5db',
  fontSize: '14px',
  boxSizing: 'border-box'
};

const btnStyle = {
  padding: '10px 14px',
  border: 'none',
  borderRadius: '6px',
  cursor: 'pointer',
  fontSize: '14px',
  fontWeight: '600',
  transition: 'opacity 0.2s'
};
