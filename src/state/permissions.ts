import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { useClient } from "./client";
import { User, useUser } from "./user";
import { Client } from "@api/client";

// if is null not loaded
export type Permissions = Set<string> | null;

export const usePermissions = create(
  subscribeWithSelector(
    combine({ permissions: null as Permissions }, (set) => ({
      update: (permissions: Set<string>) => set({ permissions: permissions }),
      reset: () => set({ permissions: new Set<string>() }),
    }))
  )
);

// derived store to handle on each change
const useUserClient = create(
  subscribeWithSelector(
    combine(
      {
        user: useUser.getState().user,
        client: useClient.getState().client,
      },
      (set) => ({
        updateUser: (user: User) => set({ user }),
        updateClient: (client: Client) => set({ client }),
      })
    )
  )
);

useUser.subscribe(
  (state) => state.user,
  (user) => {
    useUserClient.getState().updateUser(user);
  },
  { fireImmediately: true }
);

useClient.subscribe(
  (state) => state.client,
  (client) => {
    useUserClient.getState().updateClient(client);
  },
  { fireImmediately: true }
);

useUserClient.subscribe(
  (state) => ({ user: state.user, client: state.client }),
  ({ user, client }) => {
    const permissionsUpdate = usePermissions.getState().update;
    const permissionsReset = usePermissions.getState().reset;

    if (user.present && client.authorization) {
      client.users
        .getUserPermissions(user.id)
        .then((r) => permissionsUpdate(new Set(r.data.permissions)))
        .catch((e) => console.error(e));
    } else {
      permissionsReset();
    }
  },
  { fireImmediately: true }
);

usePermissions.subscribe(
  (state) => state.permissions,
  (permissions) => {
    if (import.meta.env.DEV) {
      console.log("[state.permissions]: ", permissions);
    }
  },
  { fireImmediately: true }
);
