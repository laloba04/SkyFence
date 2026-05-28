import { useNavigate } from 'react-router-dom';
import { XCircle } from 'lucide-react';

export default function SubscriptionCancel() {
  const navigate = useNavigate();

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                  height: '100vh', background: '#fff7ed', fontFamily: 'system-ui, sans-serif', padding: '32px', textAlign: 'center' }}>
      <XCircle size={72} color="#ea580c" style={{ marginBottom: '24px' }} />
      <h1 style={{ margin: '0 0 12px', fontSize: '28px', color: '#c2410c' }}>Pago cancelado</h1>
      <p style={{ margin: '0 0 32px', color: '#4b5563', fontSize: '16px' }}>
        No se ha realizado ningún cargo. Puedes intentarlo de nuevo cuando quieras.
      </p>
      <button onClick={() => navigate('/zones')}
        style={{ padding: '10px 24px', borderRadius: '8px', background: '#ea580c', color: 'white',
                 border: 'none', cursor: 'pointer', fontWeight: 600, fontSize: '15px' }}>
        Volver a Zonas
      </button>
    </div>
  );
}
