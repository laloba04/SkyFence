import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, Loader } from 'lucide-react';
import { authHeader, saveAuth, getToken } from '../auth';

const API = import.meta.env.VITE_API_URL ?? '';
const MAX_ATTEMPTS = 10;
const INTERVAL_MS = 2000;

export default function SubscriptionSuccess() {
  const navigate = useNavigate();
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    let attempts = 0;
    let stopped = false;

    async function poll() {
      while (attempts < MAX_ATTEMPTS && !stopped) {
        attempts++;
        try {
          const res = await fetch(`${API}/api/users/me`, { headers: authHeader() });
          if (res.ok) {
            const user = await res.json();
            if (user.subscriptionStatus === 'PRO') {
              saveAuth(getToken(), user);
              if (!stopped) setConfirmed(true);
              return;
            }
          }
        } catch { /* continuar */ }
        await new Promise(r => setTimeout(r, INTERVAL_MS));
      }
      // timeout: actualizar igualmente y redirigir
      if (!stopped) setConfirmed(true);
    }

    poll();
    return () => { stopped = true; };
  }, []);

  useEffect(() => {
    if (!confirmed) return;
    const t = setTimeout(() => navigate('/dashboard'), 2000);
    return () => clearTimeout(t);
  }, [confirmed, navigate]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
                  height: '100vh', background: '#f0fdf4', fontFamily: 'system-ui, sans-serif', padding: '32px', textAlign: 'center' }}>
      <CheckCircle size={72} color="#16a34a" style={{ marginBottom: '24px' }} />
      <h1 style={{ margin: '0 0 12px', fontSize: '28px', color: '#15803d' }}>¡Suscripción activada!</h1>
      <p style={{ margin: '0 0 8px', color: '#4b5563', fontSize: '16px' }}>
        Tu plan Pro está activo. Ya puedes crear zonas ilimitadas.
      </p>

      {!confirmed ? (
        <p style={{ margin: '0 0 32px', color: '#9ca3af', fontSize: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Loader size={14} style={{ animation: 'spin 1s linear infinite' }} />
          Confirmando tu suscripción…
        </p>
      ) : (
        <p style={{ margin: '0 0 32px', color: '#9ca3af', fontSize: '14px' }}>
          Redirigiendo al dashboard…
        </p>
      )}

      <button onClick={() => navigate('/dashboard')}
        style={{ padding: '10px 24px', borderRadius: '8px', background: '#16a34a', color: 'white',
                 border: 'none', cursor: 'pointer', fontWeight: 600, fontSize: '15px' }}>
        Ir al Dashboard
      </button>

      <style>{`@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}
