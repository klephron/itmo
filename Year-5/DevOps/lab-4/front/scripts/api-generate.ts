/* eslint-disable */
/* tslint:disable */
// @ts-nocheck

import * as path from "node:path";
import * as process from "node:process";
import { generateApi } from "swagger-typescript-api";

const API_DOCS_PATH = path.resolve(process.cwd(), "./src/api/api-docs.json");
const API_GEN_PATH = path.resolve(process.cwd(), "./src/api/generated/");

await generateApi({
  input: API_DOCS_PATH,
  output: API_GEN_PATH,
  modular: true,
  hooks: {
    onPrepareConfig(config: unknown) {
      config.apiConfig.baseUrl = "localhost:8081";
    },
  },
});
