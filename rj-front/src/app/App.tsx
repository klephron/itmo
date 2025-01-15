import { batch, createEffect } from 'solid-js';
import styles from './App.module.scss';

import Log from './Log';
import Map from './Map';
import View from './View';
import { clientGet } from '@state/client';
import { subscriptionGet } from '@state/subscription';
import { logsAppend } from '@state/log';
import { world, worldSet } from '@state/world';
import { reconcile } from 'solid-js/store';

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
          batch(() => {
            worldSet('postoffices', reconcile(e.args.postoffices, { key: 'id', merge: true }));
            worldSet('transports', reconcile(e.args.transports, { key: 'id', merge: true }));
            worldSet('actors', reconcile(e.args.actors, { key: 'id', merge: true }));
          });
        }
      );
    }
  });

  createEffect(() => {
    console.log(world.actors.length, world.postoffices.length, world.transports.length)
  })

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
