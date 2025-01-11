import { Message } from './message';

export interface SubscribeMessage extends Message {
  args: Args;
}

interface Args {
  ups: number;
  post_office_ids: number[];
  transport_ids: number[];
  actor_ids: number[];
}
