import { WebSocketClient } from '@websocket/client';
import { createSignal } from 'solid-js';
import { logsAppend } from './log';
import { SessionResponse } from '@websocket/event/session-response';

export const [clientGet, clientSet] = createSignal<WebSocketClient | undefined>(undefined);

export function clientOnSessionListen(e: SessionResponse) {
  logsAppend(JSON.stringify(e), true);
}

export function clientOnOpen() {
  logsAppend('websocket session opened');
}

export function clientOnClose() {
  logsAppend('websocket session closed');
}
