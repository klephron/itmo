import { CoordinatesPack } from './coordinates-pack';
import { MailPack } from './mail-pack';
import { PostalAddressPack } from './postal-address-pack';

export interface TransportPack {
  id: number;
  capacity: number;
  speed: number; // double
  ops: number; // double
  license_plate: string;
  mails: MailPack[];
  route_postoffice_ids: number[];
  loading_addresses: PostalAddressPack[];
  delivery_addresses: PostalAddressPack[];
  coordinates: CoordinatesPack[];
  at_postoffice_id: number;
}
