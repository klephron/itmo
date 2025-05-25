import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { LocalStorageKeys } from "./localstorage";

export interface User {
  id: number;
  username: string;
  permissions: Set<string>;
}

const defaultUser = { id: 0, username: "anonymous", permissions: new Set<string>() } as User;

function localStorageGet() {
  let str = localStorage.getItem(LocalStorageKeys.USER);
  if (!str) {
    return defaultUser;
  }

  try {
    const obj = JSON.parse(str);
    return {
      id: Number(obj["id"]) ?? defaultUser.id,
      username: String(obj["username"]) || defaultUser.username,
      permissions: new Set<string>(
        Array.isArray(obj.permissions) ? obj.permissions : defaultUser.permissions
      ),
    } as User;
  } catch {
    return defaultUser;
  }
}

function localStorageSet(user: User) {
  console.log("before User set", user)
  localStorage.setItem(
    LocalStorageKeys.USER,
    JSON.stringify({
      id: user.id,
      username: user.username,
      permissions: Array.from(user.permissions),
    })
  );
  console.log("after User set", user)
}

export const useUser = create(
  subscribeWithSelector(
    combine(
      {
        user: localStorageGet(),
      },
      (set) => ({
        update: (user: User) => set({ user: user }),
      })
    )
  )
);

useUser.subscribe(
  (state) => state.user,
  (user) => {
    localStorageSet(user);
  }
);
