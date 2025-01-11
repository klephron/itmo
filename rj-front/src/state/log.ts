import { createSignal } from 'solid-js';

const [_logsGet, _logsSet] = createSignal('');

export function logsAppend(text: string, isOutput?: boolean, newline: boolean = true) {
  if (isOutput != null) {
    text = (isOutput ? '< ' : '> ') + text;
  }
  text += newline ? '\n' : '';
  _logsSet(_logsGet() + text);
}

export function logsClear() {
  _logsSet('');
}

export function logsUse() {
  return _logsGet;
}
