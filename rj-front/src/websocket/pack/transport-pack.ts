import { CoordinatesPack } from './coordinates-pack';
import { MailPack } from './mail-pack';
import { PostalAddressPack } from './postal-address-pack';
import { RoutePack } from './route-pack';

export interface TransportPack {
  id: number;
  capacity: number;
  speed: number; // double
  ops: number; // double
  license_plate: string;
  mails: MailPack[];
  mails_count: number;
  route: RoutePack;
  loading_addresses: PostalAddressPack[];
  delivery_addresses: PostalAddressPack[];
  coordinates: CoordinatesPack;
  at_postoffice_id: number;
}
