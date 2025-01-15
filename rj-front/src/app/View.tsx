import { createEffect, createSignal, Match, onMount, Switch } from 'solid-js';
import styles from './index.module.scss';
import {
  clientGet,
  clientOnClose,
  clientOnOpen,
  clientOnSessionListen,
  clientSet,
} from '@state/client';
import ViewPostoffice from './ViewPostoffice';
import ViewTransport from './ViewTransport';
import ViewActor from './ViewActor';
import { WebSocketClient } from '@websocket/client';
import { SubscribeMessage } from '@websocket/message/subscribe-message';
import { subscriptionGet, subscriptionSet } from '@state/subscription';

type Props = {
  ups: number;
  serverUrl: string;
};

export default function View(props: Props) {
  const [getSelectedView, setSelectedView] = createSignal(SelectedView.POSTOFFICE);
  const [getServerURL, setServerURL] = createSignal(props.serverUrl);
  const [getUPS, setUPS] = createSignal(String(props.ups));

  onMount(() => {
    update();
  });

  function update() {
    if (clientGet() == null || clientGet()!.ws.url != getServerURL()) {
      new WebSocketClient(
        getServerURL(),
        clientOnSessionListen,
        (c) => {
          clientOnOpen();
          clientSet(c);
        },
        (_) => {
          clientOnClose();
          clientSet(undefined);
        }
      );
    }
    if (subscriptionGet() == null || subscriptionGet()!.args.ups != Number(getUPS())) {
      const ups = Number(getUPS());
      subscriptionSet({
        args: { ups: ups, postoffice_ids: [], transport_ids: [], actor_ids: [] },
      } as SubscribeMessage);
    }
  }

  return (
    <div class={styles.view}>
      <div class={styles.view_top}>
        <div class={styles.view_top_select}>
          <button onClick={() => setSelectedView(SelectedView.POSTOFFICE)}>Post Offices</button>
          <button onClick={() => setSelectedView(SelectedView.TRANSPORT)}>Transports</button>
          <button onClick={() => setSelectedView(SelectedView.ACTORS)}>Actors</button>
        </div>
        <div class={styles.view_top_config}>
          <input
            placeholder="Server URL"
            value={getServerURL()}
            onChange={(e) => setServerURL(e.target.value)}
          />
          <input
            placeholder="Updates Per Second"
            value={getUPS()}
            onChange={(e) => setUPS(e.target.value)}
          />
          <button onClick={update}>Update</button>
        </div>
      </div>
      <div class={styles.view_bottom}>
        <Switch>
          <Match when={getSelectedView() == SelectedView.POSTOFFICE}>
            <ViewPostoffice />
          </Match>
          <Match when={getSelectedView() == SelectedView.TRANSPORT}>
            <ViewTransport />
          </Match>
          <Match when={getSelectedView() == SelectedView.ACTORS}>
            <ViewActor />
          </Match>
        </Switch>
      </div>
    </div>
  );
}

enum SelectedView {
  POSTOFFICE,
  TRANSPORT,
  ACTORS,
}
