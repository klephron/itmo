import { MailPack } from './mail-pack';
import { MarkPack } from './mark-pack';

export interface TaskPack {
  id: number;
  kind: TaskPackKind;
  postoffice_id: number;
}

export const TaskPackKind = {
  SEND_MAIL_ONE: 'SEND_MAIL_ONE',
  SEND_MAIL_REPEATED: 'SEND_MAIL_REPEATED',
  RECEIVE_MAIL_ONE: 'RECEIVE_MAIL_ONE',
  RECEIVE_MAIL_REPEATED: 'RECEIVE_MAIL_REPEATED',
  SEND_MARK_REPEATED: 'SEND_MARK_REPEATED',
};
export type TaskPackKind = (typeof TaskPackKind)[keyof typeof TaskPackKind];

export interface SendMailOneTaskPack extends TaskPack {
  mail: MailPack;
}

export interface SendMailRepeatedTaskPack extends TaskPack {
  interval_ms: number;
  mail: MailPack;
}

export interface SendMarkRepeatedTaskPack extends TaskPack {
  interval_ms: number;
  n: number;
  mark: MarkPack;
}
