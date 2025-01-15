import { Message } from './message';
import { TaskPack } from '@websocket/pack';

export interface ActorAddTaskMessage extends Message {
  args: Args;
}

interface Args {
  task: TaskPack;
}
