import { logsAppend } from '@state/log';
import { createSignal } from 'solid-js';

import styles from './index.module.scss';
import { clientGet } from '@state/client';
import LogText from './LogText';

export default function Log() {
  const [getLogCommand, setLogCommand] = createSignal('');

  // won't make subscriptions
  function sendLogCommand() {
    try {
      const cmd = getLogCommand();
      logsAppend(cmd, false);
      setLogCommand('');
      const msg = JSON.parse(cmd);
      clientGet()!.sendGeneric(msg, (e) => {
        logsAppend(JSON.stringify(e), true);
      });
    } catch (error: any) {
      logsAppend(error.message);
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
    <div class={styles.log}>
      <LogText />
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
  );
}
