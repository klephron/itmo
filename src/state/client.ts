import { Client } from "@api/client";
import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { Token, useToken } from "./token";

function getAuthorization(token: Token) {
  if (token.tokenType && token.accessToken) {
    return `${token.tokenType} ${token.accessToken}`;
  }
  return undefined;
}

const useClient = create(
  subscribeWithSelector(
    combine(
      {
        client: new Client(
          import.meta.env.VITE_SERVER_URL,
          getAuthorization(useToken.getInitialState().token)
        ),
      },
      (set) => ({
        update: (client: Client) => set({ client: client }),
      })
    )
  )
);

useToken.subscribe(
  (state) => state.token,
  (token) => {
    const client = useClient.getState().client;
    const clientUpdate = useClient.getState().update;

    clientUpdate(new Client(client.baseUrl, getAuthorization(token)));
  }
);

useClient.subscribe(
  (state) => state.client,
  (client) => {
    if (import.meta.env.DEV) {
      console.log("client updated: ", client);
    }
  }
);

export { useClient };
