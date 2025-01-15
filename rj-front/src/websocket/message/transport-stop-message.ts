import { Message } from './message';

export interface TransportStopMessage extends Message {
  args: Args;
}

interface Args {
  transport_id: number;
}
