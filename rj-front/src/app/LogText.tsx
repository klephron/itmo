import { logsGet } from '@state/log';
import { createEffect } from 'solid-js';

import styles from './index.module.scss';

export default function LogText() {
  let text: HTMLTextAreaElement;

  createEffect(() => {
    text!.scrollTop = text!.scrollHeight;
  });

  return (
    <>
      <textarea ref={text!} class={styles.log_textarea} value={logsGet()} disabled></textarea>
    </>
  );
}
