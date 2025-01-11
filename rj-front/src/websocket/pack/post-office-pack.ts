import { AddressPack } from './address-pack';
import { CoordinatesPack } from './coordinates-pack';

export interface PostOfficePack {
  id: number;
  address: AddressPack;
  coordinates: CoordinatesPack;
}
