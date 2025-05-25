// import { Cases } from "./gen/Cases";
// import { Clues } from "./gen/Clues";
// import { Incidents } from "./gen/Incidents";
// import { Notifications } from "./gen/Notifications";
// import { Templates } from "./gen/Templates";
import { Test } from "./gen/Test";
// import { Users } from "./gen/Users";

export class Client {
  test: Test;

  constructor(baseUrl?: string) {
    this.test = new Test({ baseUrl: baseUrl });
  }
}
