import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { LocalStorageKeys } from "./localstorage";

export interface Token {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  issueAtMS: number;
  expiresInMS: number;
  present: boolean;
}

const loaded = {
  accessToken: "",
  refreshToken: "",
  tokenType: "",
  issueAtMS: 0,
  expiresInMS: 0,
  present: false,
} as Token;

function localStorageGet() {
  let str = localStorage.getItem(LocalStorageKeys.TOKEN);
  if (!str) {
    return loaded;
  }

  try {
    const obj = JSON.parse(str);
    return {
      accessToken: String(obj["accessToken"]),
      refreshToken: String(obj["refreshToken"]),
      tokenType: String(obj["tokenType"]),
      issueAtMS: Number(obj["issueAtMS"]),
      expiresInMS: Number(obj["expiresInMS"]),
      present: true,
    } as Token;
  } catch {
    return loaded;
  }
}

function localStorageSet(token: Token | null) {
  if (token) {
    localStorage.setItem(
      LocalStorageKeys.TOKEN,
      JSON.stringify({
        accessToken: token.accessToken,
        refreshToken: token.refreshToken,
        tokenType: token.tokenType,
        issueAtMS: token.issueAtMS,
        expiresInMS: token.expiresInMS,
      } as Token)
    );
  } else {
    localStorage.removeItem(LocalStorageKeys.TOKEN);
  }
}

export const useToken = create(
  subscribeWithSelector(
    combine({ token: localStorageGet() }, (set) => ({
      update: (token: Token) => set({ token: token }),
      reset: () => set({ token: loaded }),
    }))
  )
);

useToken.subscribe(
  (state) => state.token,
  (token) => {
    localStorageSet(token);
  }
);

useToken.subscribe(
  (state) => state.token,
  (token) => {
    if (import.meta.env.DEV) {
      console.log("[state.token]: ", token);
    }
  },
  { fireImmediately: true }
);
