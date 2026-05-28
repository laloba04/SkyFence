import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle } from 'lucide-react';

export default function SubscriptionSuccess() {
  const navigate = useNavigate();

  useEffect(() => {
    const t = setTimeout(() => navigate('/dashboard'), 5000);
    return () => clearTimeout(t);
  }, [navigate]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                  height: '100vh', background: '#f0fdf4', fontFamily: 'system-ui, sans-serif', padding: '32px', textAlign: 'center' }}>
      <CheckCircle size={72} color="#16a34a" style={{ marginBottom: '24px' }} />
      <h1 style={{ margin: '0 0 12px', fontSize: '28px', color: '#15803d' }}>¡Suscripción activada!</h1>
      <p style={{ margin: '0 0 8px', color: '#4b5563', fontSize: '16px' }}>
        Tu plan Pro está activo. Ya puedes crear zonas ilimitadas.
      </p>
      <p style={{ margin: '0 0 32px', color: '#9ca3af', fontSize: '14px' }}>
        Redirigiendo al dashboard en 5 segundos…
      </p>
      <button onClick={() => navigate('/dashboard')}
        style={{ padding: '10px 24px', borderRadius: '8px', background: '#16a34a', color: 'white',
                 border: 'none', cursor: 'pointer', fontWeight: 600, fontSize: '15px' }}>
        Ir al Dashboard
      </button>
    </div>
  );
}
