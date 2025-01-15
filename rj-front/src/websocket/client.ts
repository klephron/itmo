import { Event } from './event';
import { SessionResponse, SubscribeEvent, SubscribeResponse } from './event';
import {
  ActorsAddMessage,
  Message,
  PostofficesAddMessage,
  SubscribeMessage,
  TransportsAddMessage,
} from './message';

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
    this.dispatcher.listen('session', onSessionListen, () => true);

    this.ws = new WebSocket(url);
    this.ws.onmessage = (e) => {
      const event: Event = JSON.parse(e.data);
      const callbacks = this.dispatcher.extract(event.event, event);
      callbacks.forEach((callback) => callback(event));
    };

    this.ws.onopen = () => onopen(this);
    this.ws.onclose = () => onclose(this);
  }

  /* GENERIC API METHODS */
  sendGenericOnetime<T extends Event>(
    data: Message,
    onResponse: (e: T) => void = (_) => {},
    onResponsePredicate: (e: T) => boolean = (_) => true
  ) {
    this.dispatcher.onetime(data.op!, onResponse, onResponsePredicate);
    this.ws.send(JSON.stringify(data));
  }

  sendGenericListen<T extends Event, U extends Event>(
    data: Message,
    onResponse: (e: T) => void = (_) => {},
    onEvent: (e: U) => void = (_) => {},
    onResponsePredicate: (e: T) => boolean,
    OnEventPredicate: (e: U) => boolean
  ) {
    this.dispatcher.onetime(data.op!, onResponse, onResponsePredicate);
    this.dispatcher.listen(data.op!, onEvent, OnEventPredicate);
    const str = JSON.stringify(data);
    this.ws.send(str);
  }

  /* CUSTOM METHOD APIS */
  subscribe(
    data: SubscribeMessage,
    onResponse: (e: SubscribeResponse) => void,
    onEvent: (e: SubscribeEvent) => void
  ) {
    data.op = 'subscribe';
    this.sendGenericListen(
      data,
      onResponse,
      onEvent,
      (e: SubscribeResponse) => e.status != null,
      (e: SubscribeEvent) => (e as any as SubscribeResponse).status == null
    );
  }

  unsubscribe() {
    this.dispatcher.callbacks.delete('subscribe');
  }

  postofficesAdd(data: PostofficesAddMessage) {
    data.op = 'postoffices.add';
    this.sendGenericOnetime(data);
  }

  transportsAdd(data: TransportsAddMessage) {
    data.op = 'transports.add';
    this.sendGenericOnetime(data);
  }

  actorsAdd(data: ActorsAddMessage) {
    data.op = 'actors.add';
    this.sendGenericOnetime(data);
  }
}

class Dispatcher {
  callbacks: Map<string, DispatcherCallback[]>;

  constructor() {
    this.callbacks = new Map();
  }

  // add onetime callback
  public onetime(op: string, callback: (e: any) => void, predicate: (e: any) => boolean) {
    this.insert(op, callback, true, predicate);
  }

  // add stream event callback
  public listen(op: string, callback: (e: any) => void, predicate: (e: any) => boolean) {
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
