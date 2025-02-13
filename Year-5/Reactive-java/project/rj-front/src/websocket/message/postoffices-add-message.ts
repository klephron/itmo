import { AddressPack } from '@websocket/pack/address-pack';
import { Message } from './message';
import { CoordinatesPack } from '@websocket/pack/coordinates-pack';

export interface PostofficesAddMessage extends Message {
  args: Args;
}

interface Args {
  address: AddressPack;
  coordinates: CoordinatesPack;
}
