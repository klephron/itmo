import { Event } from './event';

export interface ErrorResponse extends Event {
  data: object;
  msg: string;
}
