import { useEffect, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export function useWebSocket() {
  const [alerts, setAlerts] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body);
          setAlerts(prev => [alert, ...prev].slice(0, 50));
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    return () => client.deactivate();
  }, []);

  return { alerts, connected };
}
