import { createSignal, Match, onMount, Switch } from 'solid-js';
import styles from './index.module.scss';
import { clientGet, clientOnClose, clientOnSessionListen, clientSet } from '@state/client';
import ViewPostOffice from './ViewPostOffice';
import ViewTransport from './ViewTransport';
import ViewActor from './ViewActor';
import { WebSocketClient } from '@websocket/client';
import { logsAppend } from '@state/log';
import { SubscribeMessage } from '@websocket/message/subscribe-message';

type Props = {
  ups: number;
  serverUrl: string;
};

export default function View(props: Props) {
  const [getSelectedView, setSelectedView] = createSignal(SelectedView.POST_OFFICE);
  const [getServerURL, setServerURL] = createSignal(props.serverUrl);
  const [getUPS, setUPS] = createSignal(String(props.ups));

  // connect while starting
  onMount(() => {
    updateSubscription();
  });

  function subscribeClient(client: WebSocketClient) {
    const data: SubscribeMessage = {
      args: {
        ups: Number(getUPS()),
        post_office_ids: [],
        transport_ids: [],
        actor_ids: [],
      },
    };
    client.unsubscribe();
    client.subscribe(
      data,
      (e) => {
        logsAppend(JSON.stringify(e), true);
      },
      (e) => {
        console.log(e);
      }
    );
  }

  function updateSubscription() {
    if (clientGet() == null || clientGet()!.ws.url != getServerURL()) {
      clientSet(
        new WebSocketClient(getServerURL(), clientOnSessionListen, subscribeClient, clientOnClose)
      );
    } else if (clientGet() != null) {
      subscribeClient(clientGet()!);
    }
  }

  return (
    <div class={styles.view}>
      <div class={styles.view_top}>
        <div class={styles.view_select}>
          <button onClick={() => setSelectedView(SelectedView.POST_OFFICE)}>Post Offices</button>
          <button onClick={() => setSelectedView(SelectedView.TRANSPORT)}>Transports</button>
          <button onClick={() => setSelectedView(SelectedView.ACTORS)}>Actors</button>
        </div>
        <div class={styles.view_config}>
          <input
            class={styles.view_config_server}
            placeholder="Server URL"
            value={getServerURL()}
            onChange={(e) => setServerURL(e.target.value)}
          />
          <input
            class={styles.view_config_ups}
            placeholder="Updates Per Second"
            value={getUPS()}
            onChange={(e) => setUPS(e.target.value)}
          />
          <button onClick={updateSubscription}>Update</button>
        </div>
      </div>
      <div class={styles.view_data}>
        <Switch>
          <Match when={getSelectedView() == SelectedView.POST_OFFICE}>
            <ViewPostOffice />
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
  POST_OFFICE,
  TRANSPORT,
  ACTORS,
}
