import { defineConfig } from "vite";
// @ts-ignore
import { fileURLToPath } from "url";
import solid from "vite-plugin-solid";

export default defineConfig({
  plugins: [solid()],
  test: {
    environment: "jsdom",
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      reportsDirectory: "./coverage",
      include: ["src/**/*.{js,jsx,ts,tsx}"],
      exclude: [
        "src/**/*.test.{js,jsx,ts,tsx}",
        "node_modules/**",
        "src/api/generated/**",
        "src/index.tsx",
      ],
    },
  },
  resolve: {
    conditions: ["development", "browser"],
    alias: [
      {
        find: "@api",
        replacement: fileURLToPath(new URL("./src/api/", import.meta.url)),
      },
      {
        find: "@assets",
        replacement: fileURLToPath(new URL("./src/assets/", import.meta.url)),
      },
      {
        find: "@components",
        replacement: fileURLToPath(
          new URL("./src/components/", import.meta.url),
        ),
      },
      {
        find: "@state",
        replacement: fileURLToPath(new URL("./src/state/", import.meta.url)),
      },
    ],
  },
});
