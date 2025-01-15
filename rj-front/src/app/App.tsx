import { createEffect } from 'solid-js';
import styles from './index.module.scss';

import Log from './Log';
import Map from './Map';
import View from './View';
import { clientGet } from '@state/client';
import { subscriptionGet } from '@state/subscription';
import { logsAppend } from '@state/log';

const DEFAULT_SERVER_URL: string = 'ws://localhost:8088/ws';
const DEFAULT_UPS: number = 1;

export default function App() {
  // subscribe client on change
  createEffect(() => {
    if (clientGet() != null && subscriptionGet() != null) {
      const client = clientGet()!;
      const subscription = subscriptionGet()!;
      client.unsubscribe();
      client.subscribe(
        subscription,
        (e) => {
          logsAppend(JSON.stringify(e), true);
        },
        (e) => {
          console.log(e);
        }
      );
    }
  });

  return (
    <>
      <h1>Mail Simulation</h1>
      <div class={styles.outer}>
        <div class={styles.top}>
          <Map />
          <Log />
        </div>
        <View ups={DEFAULT_UPS} serverUrl={DEFAULT_SERVER_URL} />
      </div>
    </>
  );
}
