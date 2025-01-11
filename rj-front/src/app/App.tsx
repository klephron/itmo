import { WebSocketClient } from '@websocket/client';
import { SessionResponse } from '@websocket/event/session-response';
import { createSignal } from 'solid-js';

import styles from './App.module.scss';

export default function App() {
  const [getLogTextAreaText, setLogTextAreaText] = createSignal('');
  const [getLogCommand, setLogCommand] = createSignal('');

  const client = new WebSocketClient('ws://localhost:8088/ws', onSessionListen, onOpen, onClose);

  function appendLog(text: string, isOutput?: boolean, newline: boolean = true) {
    if (isOutput != null) {
      text = (isOutput ? '< ' : '> ') + text;
    }
    text += newline ? '\n' : '';
    setLogTextAreaText(getLogTextAreaText() + text);
  }

  function onSessionListen(e: SessionResponse) {
    appendLog(JSON.stringify(e), true);
  }

  function onOpen() {
    appendLog('websocket session opened');
  }

  function onClose() {
    appendLog('websocket session closed');
  }

  // won't make subscriptions
  function sendLogCommand() {
    try {
      const cmd = getLogCommand();
      appendLog(cmd, false)
      setLogCommand('');
      const msg = JSON.parse(cmd);
      client.sendGeneric(msg, (e) => {
        appendLog(JSON.stringify(e), true);
      });
    } catch (error: any) {
      appendLog(error.message);
    }
  }

  function onLogCommandChange(e: any) {
    setLogCommand(e.target.value);
    if (e.key === 'Enter') {
      sendLogCommand();
      setLogCommand('');
    }
  }

  return (
    <>
      <h1>Reactive Java View</h1>
      <div class={styles.outer}>
        <div class={styles.top}>
          <div class={styles.map}>TODO: MAP</div>
          <div class={styles.log}>
            <textarea class={styles.log_textarea} value={getLogTextAreaText()} disabled></textarea>
            <div class={styles.log_input}>
              <button onClick={sendLogCommand}>Send</button>
              <input
                class={styles.log_input}
                placeholder="Enter websocket request"
                value={getLogCommand()}
                onKeyDown={onLogCommandChange}
              />
            </div>
          </div>
        </div>
        <div class={styles.view}>TODO: VIEW</div>
      </div>
    </>
  );
}
