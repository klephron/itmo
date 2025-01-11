import { Event } from './event';

export interface SessionResponse extends Event {
  args: Args;
}

export interface Args {
  session_id: string;
  status: Status;
}

export const Status = {
  CREATED: 'CREATED',
};
export type Status = (typeof Status)[keyof typeof Status];
