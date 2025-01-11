import { Event } from './event/event';
import { SessionResponse } from './event/session-response';
import { SubscribeEvent } from './event/subscribe-event';
import { SubscribeResponse } from './event/subscribe-response';
import { Message } from './message/message';
import { SubscribeMessage } from './message/subscribe-message';

export class WebSocketClient {
  ws: WebSocket;
  dispatcher: Dispatcher;

  // backend guarantees to return events
  constructor(
    url: string,
    onSessionListen: (e: SessionResponse) => void,
    onopen: (ws: WebSocketClient) => void,
    onclose: (ws: WebSocketClient) => void
  ) {
    this.dispatcher = new Dispatcher();
    this.dispatcher.listen('session', onSessionListen);

    this.ws = new WebSocket(url);
    this.ws.onmessage = (e) => {
      const event: Event = JSON.parse(e.data);
      const callbacks = this.dispatcher.extract(event.event, event);
      callbacks.forEach((callback) => callback(event));
    };

    this.ws.onopen = () => onopen(this);
    this.ws.onclose = () => onclose(this);
  }

  /* API METHODS */
  sendGeneric(data: Message, callback: (e: Event) => void) {
    this.dispatcher.onetime(data.op!, callback);
    this.ws.send(JSON.stringify(data));
  }

  subscribe(
    data: SubscribeMessage,
    response: (e: SubscribeResponse) => void,
    callback: (e: SubscribeEvent) => void
  ) {
    data.op = 'subscribe';
    this.dispatcher.onetime(data.op, response, (e: SubscribeResponse) => {
      return e.status != null;
    });
    this.dispatcher.listen(data.op, callback, (e: SubscribeResponse) => {
      return e.status == null;
    });
    const str = JSON.stringify(data);
    this.ws.send(str);
  }

  unsubscribe() {
    this.dispatcher.callbacks.delete('subscribe');
  }
}

class Dispatcher {
  callbacks: Map<string, DispatcherCallback[]>;

  constructor() {
    this.callbacks = new Map();
  }

  // add onetime callback
  public onetime(
    op: string,
    callback: (e: any) => void,
    predicate: (e: any) => boolean = (_) => true
  ) {
    this.insert(op, callback, true, predicate);
  }

  // add stream event callback
  public listen(
    op: string,
    callback: (e: any) => void,
    predicate: (e: any) => boolean = (_) => true
  ) {
    this.insert(op, callback, false, predicate);
  }

  // inserts callbacks inside
  private insert(
    op: string,
    callback: (e: any) => void,
    onetime: boolean,
    predicate: (e: any) => boolean = (_) => true
  ) {
    let arr = this.callbacks.get(op);
    if (arr == null) {
      arr = [];
      this.callbacks.set(op, arr);
    }
    arr.push({
      callback: callback,
      onetime: onetime,
      predicate: predicate,
    } as DispatcherCallback);
  }

  // returns all callback that are stored, removing them if onetime
  public extract(op: string, e: any) {
    const callbacks: ((e: any) => void)[] = [];

    const arr = this.callbacks.get(op);
    if (arr == null) {
      return callbacks;
    }

    for (let i = 0; i < arr.length; ++i) {
      if (arr[i].predicate(e)) {
        if (arr[i].onetime) {
          callbacks.push(arr.splice(i, 1)[0].callback);
          i -= 1;
        } else {
          callbacks.push(arr[i].callback);
        }
      }
    }
    return callbacks;
  }

  // removes all callbacks that match
  public remove(op: string, e: any) {
    const arr = this.callbacks.get(op);
    if (arr == null) {
      return;
    }
    for (let i = 0; i < arr.length; ++i) {
      if (arr[i].predicate(e)) {
        arr.splice(i, 1);
        i -= 1;
      }
    }
  }
}

export interface DispatcherCallback {
  callback: (e: any) => void;
  onetime: boolean; // will be deleted when callback is called
  predicate: (e: any) => boolean; // select in arr
}
