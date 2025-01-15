import { Event } from './event';

export interface SessionResponse extends Event {
  args: Args;
}

export interface Args {
  session_id: string;
  status: SessionResponseStatus;
}

export const SessionResponseStatus = {
  CREATED: 'CREATED',
};
export type SessionResponseStatus =
  (typeof SessionResponseStatus)[keyof typeof SessionResponseStatus];
