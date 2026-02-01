import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { LocalStorageKeys } from "./localstorage";

export interface User {
  id: number;
  username: string;
  present: boolean;
}

const loaded = { id: 0, username: "", present: false } as User;

function localStorageGet() {
  let str = localStorage.getItem(LocalStorageKeys.USER);
  if (!str) {
    return loaded;
  }

  try {
    const obj = JSON.parse(str);
    return {
      id: Number(obj["id"]),
      username: String(obj["username"]),
      present: true,
    } as User;
  } catch {
    return loaded;
  }
}

function localStorageSet(user: User | null) {
  if (user) {
    localStorage.setItem(
      LocalStorageKeys.USER,
      JSON.stringify({
        id: user.id,
        username: user.username,
      })
    );
  } else {
    localStorage.removeItem(LocalStorageKeys.USER);
  }
}

export const useUser = create(
  subscribeWithSelector(
    combine({ user: localStorageGet() }, (set) => ({
      update: (user: User) => set({ user: user }),
      reset: () => set({ user: loaded }),
    }))
  )
);

useUser.subscribe(
  (state) => state.user,
  (user) => {
    localStorageSet(user);
  }
);

useUser.subscribe(
  (state) => state.user,
  (user) => {
    if (import.meta.env.DEV) {
      console.log("[state.user]: ", user);
    }
  },
  { fireImmediately: true }
);
