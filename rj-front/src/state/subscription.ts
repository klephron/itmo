import { SubscribeMessage } from '@websocket/message/subscribe-message';
import { createSignal } from 'solid-js';

export const [subscriptionGet, subscriptionSet] = createSignal<SubscribeMessage | undefined>(
  undefined
);
