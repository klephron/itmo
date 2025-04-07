import { defineConfig } from "vite";
import solidPlugin from "vite-plugin-solid";

export default defineConfig({
  plugins: [solidPlugin()],
  build: {
    target: "esnext",
  },
  test: {
    globals: true,
    environment: "jsdom",
    reporters: ["verbose"],
  },
  resolve: {
    conditions: ["development", "browser"],
  },
});
