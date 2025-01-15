import { AddressPack } from './address-pack';
import { MailPack } from './mail-pack';
import { TaskPack } from './task-pack';

export interface ActorPack {
  kind: ActorPackKind;
  id?: number;
  address: AddressPack;
  tasks?: TaskPack[];
  mails?: MailPack[];
  mails_count?: number;
  mails_buffer_count?: number;
  mails_buffer_capacity?: number;
}

export const ActorPackKind = {
  INDIVIDUAL: 'INDIVIDUAL',
  ORGANIZATION: 'ORGANIZATION',
};
export type ActorPackKind = (typeof ActorPackKind)[keyof typeof ActorPackKind];
