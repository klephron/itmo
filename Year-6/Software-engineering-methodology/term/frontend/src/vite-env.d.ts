/// <reference types="vite/client" />

interface ImportMetaEnv {
  VITE_SERVER_URL: string;
  MODE: string;
  DEV: string;
  PROD: string;
  SSR: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
