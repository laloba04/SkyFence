export default function ConfirmModal({ isOpen, title, message, onConfirm, onCancel }) {
  if (!isOpen) return null;

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100vw', height: '100vh',
      background: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center', zIndex: 9999
    }}>
      <div style={{
        background: 'white', padding: '24px', borderRadius: '12px', width: '340px',
        boxShadow: '0 10px 15px -3px rgba(0,0,0,0.1), 0 4px 6px -2px rgba(0,0,0,0.05)',
        fontFamily: 'system-ui, -apple-system, sans-serif'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
          <div style={{ width: '40px', height: '40px', borderRadius: '50%', background: '#fee2e2', display: 'flex', justifyContent: 'center', alignItems: 'center', color: '#dc2626', fontSize: '20px' }}>
            ⚠️
          </div>
          <h2 style={{ margin: 0, fontSize: '18px', color: '#1f2937' }}>{title}</h2>
        </div>
        <p style={{ margin: '0 0 24px 0', fontSize: '14px', color: '#4b5563', lineHeight: '1.5' }}>
          {message}
        </p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
          <button onClick={onCancel} style={{ padding: '8px 16px', background: '#f3f4f6', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: '500', color: '#374151', transition: 'background 0.2s' }}>
            Cancelar
          </button>
          <button onClick={onConfirm} style={{ padding: '8px 16px', background: '#dc2626', border: 'none', borderRadius: '6px', cursor: 'pointer', fontSize: '14px', fontWeight: '500', color: 'white', transition: 'background 0.2s' }}>
            Eliminar
          </button>
        </div>
      </div>
    </div>
  );
}
