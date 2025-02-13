import { PostofficePack } from '@websocket/pack/postoffice-pack';
import { Event } from './event';
import { TransportPack } from '@websocket/pack/transport-pack';
import { ActorPack } from '@websocket/pack/actor-pack';

export interface SubscribeEvent extends Event {
  args: Args;
}

interface Args {
  session_id: string;
  postoffices: PostofficePack[];
  transports: TransportPack[];
  actors: ActorPack[];
}
