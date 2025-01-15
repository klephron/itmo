import { ActorPack } from '@websocket/pack/actor-pack';
import { PostofficePack } from '@websocket/pack/postoffice-pack';
import { TransportPack } from '@websocket/pack/transport-pack';
import { createStore } from 'solid-js/store/types/server.js';

export const [world, worldSet] = createStore({
  postoffices: <PostofficePack[]>[],
  transports: <TransportPack[]>[],
  actors: <ActorPack[]>[],
});
