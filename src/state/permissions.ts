import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { useClient } from "./client";
import { User, useUser } from "./user";
import { Client } from "@api/client";
import { useToken } from "./token";

// if is null not loaded
export type Permissions = Set<string> | null;

function matchesPermission(
  pattern: string,
  required: string,
  resourceOwnerId?: number,
  currentUserId?: number,
) {
  const patParts = pattern.split(":");
  const reqParts = required.split(":");
  if (patParts.length !== 3 || reqParts.length !== 3) {
    return false;
  }

  const [patScope, patObj, patPriv] = patParts;
  const [reqScope, reqObj, reqPriv] = reqParts;

  if (!(patScope === "*" || patScope === reqScope)) {
    return false;
  }
  if (!(patPriv === "*" || patPriv === reqPriv)) {
    return false;
  }

  if (patObj === "*") {
    return true;
  }

  if (patObj === "$") {
    if (resourceOwnerId === undefined || currentUserId === undefined) {
      return false;
    }
    // Разрешено, если ресурс принадлежит текущему пользователю
    return resourceOwnerId === currentUserId;
  }

  return patObj === reqObj;
}

export function hasPermission(required: string, resourceOwnerId?: number): boolean {
  const permissions = usePermissions.getState().permissions;
  const currentUser = useUser.getState().user;
  const currentUserId = currentUser.present ? currentUser.id : undefined;
  if (!permissions) {
    return false;
  }
  for (const perm of permissions) {
    if (matchesPermission(perm, required, resourceOwnerId, currentUserId)) {
      return true;
    }
  }
  return false;
}

export function hasAnyPermission(required: string[], ownerId?: number): boolean {
  return required.some((p) => hasPermission(p, ownerId));
}

export const usePermissions = create(
  subscribeWithSelector(
    combine({ permissions: null as Permissions }, (set) => ({
      update: (permissions: Set<string>) => set({ permissions: permissions }),
      reset: () => set({ permissions: null }),
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
        .catch((e) => {
          switch (e.status) {
            // TODO: a little bit buggy when token dies
            case 401:
              useUser.getState().reset();
              useToken.getState().reset();
              break;
            default:
              console.error(e);
          }
        });
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
