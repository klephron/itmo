import { ActorPack } from '@websocket/pack/actor-pack';
import { Message } from './message';
import { IndividualActorPack, OrganizationActorPack } from '@websocket/pack';

export interface ActorsAddMessage extends Message {
  args: Args;
}

interface Args {
  actor: ActorPack | IndividualActorPack | OrganizationActorPack;
}
