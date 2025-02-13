import { MarkPack } from './mark-pack';

export interface MailPack {
  id?: number;
  subject: string;
  sender_actor_id: number;
  receiver_actor_id: number;
  text: string;
  value?: number;
  marks?: MarkPack[];
  status?: Status;
  created_at?: string;
  applied_at?: string;
  sent_at?: string;
  received_at?: string;
}

export const Status = {
  CREATED: 'CREATED',
  APPLIED: 'APPLIED',
  SENT: 'SENT',
  RECEIVED: 'RECEIVED',
};
export type Status = (typeof Status)[keyof typeof Status];
