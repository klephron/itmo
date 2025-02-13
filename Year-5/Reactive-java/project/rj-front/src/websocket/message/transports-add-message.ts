import { Message } from './message';
import { CoordinatesPack } from '@websocket/pack/coordinates-pack';

export interface TransportsAddMessage extends Message {
  args: Args;
}

interface Args {
  capacity: number;
  speed: number;
  ops: number;
  license_plate: string;
  coordinates: CoordinatesPack;
}
