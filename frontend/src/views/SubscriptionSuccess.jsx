import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { CheckCircle, Loader } from 'lucide-react';
import { authHeader, saveAuth, getToken } from '../auth';

const API = import.meta.env.VITE_API_URL ?? '';
const MAX_ATTEMPTS = 12;
const INTERVAL_MS = 2500;

export default function SubscriptionSuccess() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    let stopped = false;

    async function confirm() {
      const sessionId = searchParams.get('session_id');

      // 1. Try direct session verification (fast path, no webhook needed)
      if (sessionId) {
        try {
          const res = await fetch(
            `${API}/api/checkout/verify?session_id=${encodeURIComponent(sessionId)}`,
            { headers: authHeader() }
          );
          if (res.ok) {
            const data = await res.json();
            if (data.upgraded) {
              await refreshUserAndFinish();
              return;
            }
          }
        } catch { /* fall through to polling */ }
      }

      // 2. Fallback: poll /api/users/me until PRO (webhook may still be processing)
      let attempts = 0;
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

      // Timeout: redirect anyway
      if (!stopped) setConfirmed(true);
    }

    async function refreshUserAndFinish() {
      try {
        const res = await fetch(`${API}/api/users/me`, { headers: authHeader() });
        if (res.ok) {
          const user = await res.json();
          saveAuth(getToken(), user);
        }
      } catch { /* si falla, el localStorage igual se actualizó en verify */ }
      if (!stopped) setConfirmed(true);
    }

    confirm();
    return () => { stopped = true; };
  }, [searchParams]);

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
