import { AddressPack } from './address-pack';
import { CoordinatesPack } from './coordinates-pack';
import { MailPack } from './mail-pack';

export interface PostofficePack {
  id: number;
  address: AddressPack;
  coordinates: CoordinatesPack;
  terminal_mails: MailPack[];
  intermediate_mails: MailPack[];
  terminal_mails_count: number;
  intermediate_mails_count: number;
  transports_queued: number;
  transports_processed: number;
}
