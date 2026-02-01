import { Login } from "./gen/Login";
import { Cases } from "./gen/Cases";
import { Clues } from "./gen/Clues";
import { Incidents } from "./gen/Incidents";
import { Notifications } from "./gen/Notifications";
import { Templates } from "./gen/Templates";
import { Test } from "./gen/Test";
import { Users } from "./gen/Users";
import { ApiConfig } from "./gen/http-client";
import { Suspects } from "./gen/Suspects";
import { Logs } from "./gen/Logs";

interface SecurityData {
  anonymous?: boolean;
  authorization?: string;
}

export class Client {
  authorization?: string;
  baseUrl?: string;

  auth: Login<SecurityData>;
  cases: Cases<SecurityData>;
  clues: Clues<SecurityData>;
  suspects: Suspects<SecurityData>;
  incidents: Incidents<SecurityData>;
  notifications: Notifications<SecurityData>;
  templates: Templates<SecurityData>;
  users: Users<SecurityData>;
  test: Test<SecurityData>;
  logs: Logs<SecurityData>;

  constructor(baseUrl?: string, authorization?: string) {
    this.baseUrl = baseUrl;
    this.authorization = authorization;

    // NOTE: securityWorker is called if secure=true, otherwise not
    // e.g. client.auth .login( { username: username, password: password, }, { secure: false })
    // to set security data use setSecurityData
    // e.g. this.auth.setSecurityData()
    const config = {
      baseUrl: baseUrl,
      securityWorker: (securityData) => {
        if (securityData?.anonymous) {
          return {};
        }
        if (securityData?.authorization) {
          return {
            headers: {
              Authorization: securityData?.authorization,
            },
          };
        }
        if (this.authorization) {
          return {
            headers: {
              Authorization: this.authorization,
            },
          };
        }
        return {};
      },
    } as ApiConfig<SecurityData>;

    this.auth = new Login(config);
    this.cases = new Cases(config);
    this.clues = new Clues(config);
    this.suspects = new Suspects(config);
    this.incidents = new Incidents(config);
    this.notifications = new Notifications(config);
    this.templates = new Templates(config);
    this.users = new Users(config);
    this.test = new Test(config);
    this.logs = new Logs(config);
  }
}
