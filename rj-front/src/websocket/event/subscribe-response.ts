import { Event } from './event';

export interface SubscribeResponse extends Event {
  args: Args;
  status: Status;
  msg: string;
}

interface Args {
  ups: number;
  post_office_ids: number[];
  transport_ids: number[];
  actor_ids: number[];
}

export const Status = {
  CREATED: 'CREATED',
  RECREATED: 'RECREATED',
};
export type Status = (typeof Status)[keyof typeof Status];
