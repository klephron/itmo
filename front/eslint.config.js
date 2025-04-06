import js from "@eslint/js";
import ts from "typescript-eslint";
import solid from "eslint-plugin-solid";
import * as tsParser from "@typescript-eslint/parser";

export default [
  js.configs.recommended,
  ...ts.configs.recommended,
  {
    files: ["**/*.{ts,tsx}"],
    ...solid.configs["flat/typescript"],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        project: "tsconfig.json",
      },
    },
  },
  {
    ignores: ["dist/**", "node_modules/**"],
  },
];
