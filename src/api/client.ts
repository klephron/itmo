import { Login } from "./gen/Login";
import { Cases } from "./gen/Cases";
import { Clues } from "./gen/Clues";
import { Incidents } from "./gen/Incidents";
import { Notifications } from "./gen/Notifications";
import { Templates } from "./gen/Templates";
import { Test } from "./gen/Test";
import { Users } from "./gen/Users";

export class Client {
  auth: Login;
  cases: Cases;
  clues: Clues;
  incidents: Incidents;
  notifications: Notifications;
  templates: Templates;
  users: Users;
  test: Test;

  constructor(baseUrl?: string) {
    this.auth = new Login({ baseUrl: baseUrl });
    this.cases = new Cases({ baseUrl: baseUrl });
    this.clues = new Clues({ baseUrl: baseUrl });
    this.incidents = new Incidents({ baseUrl: baseUrl });
    this.notifications = new Notifications({ baseUrl: baseUrl });
    this.templates = new Templates({ baseUrl: baseUrl });
    this.users = new Users({ baseUrl: baseUrl });
    this.test = new Test({ baseUrl: baseUrl });
  }
}
