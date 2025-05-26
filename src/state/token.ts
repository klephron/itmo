import { create } from "zustand";
import { combine, subscribeWithSelector } from "zustand/middleware";
import { LocalStorageKeys } from "./localstorage";

export interface Token {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  issueAtMS: number;
  expiresInMS: number;
}

const defaultToken = {
  accessToken: "",
  refreshToken: "",
  tokenType: "",
  issueAtMS: Date.now(),
  expiresInMS: 0,
} as Token;

function localStorageGet() {
  let str = localStorage.getItem(LocalStorageKeys.USER);
  if (!str) {
    return defaultToken;
  }

  try {
    const obj = JSON.parse(str);
    return {
      accessToken: String(obj["accessToken"]) || defaultToken.accessToken,
      refreshToken: String(obj["refreshToken"]) || defaultToken.refreshToken,
      tokenType: String(obj["tokenType"]) || defaultToken.tokenType,
      issueAtMS: Number(obj["startTimeMS"]) || defaultToken.issueAtMS,
      expiresInMS: Number(obj["expiresInMS"]) || defaultToken.expiresInMS,
    } as Token;
  } catch {
    return defaultToken;
  }
}

function localStorageSet(token: Token) {
  localStorage.setItem(
    LocalStorageKeys.USER,
    JSON.stringify({
      accessToken: token.accessToken,
      refreshToken: token.refreshToken,
      tokenType: token.tokenType,
      issueAtMS: token.issueAtMS,
      expiresInMS: token.expiresInMS,
    } as Token)
  );
}

export const useToken = create(
  subscribeWithSelector(
    combine({ token: localStorageGet() }, (set) => ({
      update: (token: Token) => set({ token: token }),
      reset: () => set({ token: defaultToken }),
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
      console.log("token updated: ", token);
    }
  }
);
