import { RoutePack } from '@websocket/pack';
import { Message } from './message';

export interface TransportSetRouteMessage extends Message {
  args: Args;
}

interface Args {
  transport_id: number;
  route: RoutePack;
}
