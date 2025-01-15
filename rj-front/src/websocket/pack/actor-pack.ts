import { AddressPack } from './address-pack';
import { TaskPack } from './task-pack';

export interface ActorPack {
  kind: ActorPackKind;
  id: number;
  address: AddressPack;
  tasks: TaskPack[];
}

export const ActorPackKind = {
  INDIVIDUAL: 'INDIVIDUAL',
  ORGANIZATION: 'ORGANIZATION',
};
export type ActorPackKind = (typeof ActorPackKind)[keyof typeof ActorPackKind];
