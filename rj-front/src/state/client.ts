import { WebSocketClient } from '@websocket/client';
import { createSignal } from 'solid-js';

const [_clientGet, _clientSet] = createSignal<WebSocketClient | undefined>(undefined);

export function clientUse() {
  return _clientGet;
}

export function clientSet(e: WebSocketClient) {
  return _clientSet(e);
}
