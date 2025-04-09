import { defineConfig } from "vite";
// @ts-ignore
import { fileURLToPath } from "url";
import solid from "vite-plugin-solid";

export default defineConfig({
  plugins: [solid()],
  test: {
    environment: "jsdom",
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
