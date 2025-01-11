import { createSignal } from 'solid-js';

const [logsGet, logsSet] = createSignal('');

export function logsAppend(text: string, isOutput?: boolean, newline: boolean = true) {
  if (isOutput != null) {
    text = (isOutput ? '< ' : '> ') + text;
  }
  text += newline ? '\n' : '';
  logsSet(logsGet() + text);
}

export function logsClear() {
  logsSet('');
}

export function logsUse() {
  return logsGet;
}
