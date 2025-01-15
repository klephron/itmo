import { AddressPack } from './address-pack';
import { CoordinatesPack } from './coordinates-pack';

export interface PostofficePack {
  id: number;
  address: AddressPack;
  coordinates: CoordinatesPack;
}
