import { PostOfficePack } from '@websocket/pack/post-office-pack';
import { Event } from './event';
import { TransportPack } from '@websocket/pack/transport-pack';
import { ActorPack } from '@websocket/pack/actor-pack';

export interface SubscribeEvent extends Event {
  args: Args;
}

interface Args {
  session_id: string;
  post_offices: PostOfficePack[];
  transports: TransportPack[];
  actors: ActorPack[];
}
