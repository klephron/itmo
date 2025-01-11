import { WebSocketClient } from '@websocket/client';
import { SessionResponse } from '@websocket/event/session-response';

import styles from './index.module.scss';

import LogInfo from './LogInfo';
import { logsAppend } from '@state/log';
import { clientSet } from '@state/client';

export default function App() {
  function onSessionListen(e: SessionResponse) {
    logsAppend(JSON.stringify(e), true);
  }

  function onOpen() {
    logsAppend('websocket session opened');
  }

  function onClose() {
    logsAppend('websocket session closed');
  }

  clientSet(new WebSocketClient('ws://localhost:8088/ws', onSessionListen, onOpen, onClose));

  return (
    <>
      <h1>Reactive Java View</h1>
      <div class={styles.outer}>
        <div class={styles.top}>
          <div class={styles.map}>TODO: MAP</div>
          <LogInfo />
        </div>
        <div class={styles.view}>TODO: VIEW</div>
      </div>
    </>
  );
}
