import { Event } from './event';

export interface SubscribeResponse extends Event {
  args: Args;
  status: SubscribeResponseStatus;
  msg: string;
}

interface Args {
  ups: number;
  postoffice_ids: number[];
  transport_ids: number[];
  actor_ids: number[];
}

export const SubscribeResponseStatus = {
  CREATED: 'CREATED',
  RECREATED: 'RECREATED',
};
export type SubscribeResponseStatus =
  (typeof SubscribeResponseStatus)[keyof typeof SubscribeResponseStatus];
