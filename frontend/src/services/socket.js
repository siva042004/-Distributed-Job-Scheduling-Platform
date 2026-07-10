import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const WS_URL = process.env.REACT_APP_WS_URL || 'http://localhost:8080/ws';

let client = null;

export function connectSocket({ onJobUpdate, onAlert, onMetrics }) {
  client = new Client({
    webSocketFactory: () => new SockJS(WS_URL),
    reconnectDelay: 4000,
    onConnect: () => {
      client.subscribe('/topic/jobs', (message) => {
        onJobUpdate && onJobUpdate(JSON.parse(message.body));
      });
      client.subscribe('/topic/alerts', (message) => {
        onAlert && onAlert(JSON.parse(message.body));
      });
      client.subscribe('/topic/metrics', (message) => {
        onMetrics && onMetrics(JSON.parse(message.body));
      });
    },
  });
  client.activate();
  return client;
}

export function disconnectSocket() {
  if (client) client.deactivate();
}
