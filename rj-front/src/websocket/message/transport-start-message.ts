import { Message } from './message';

export interface TransportStartMessage extends Message {
  args: Args;
}

interface Args {
  transport_id: number;
  index: number;
}
