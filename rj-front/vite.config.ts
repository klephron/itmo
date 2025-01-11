import { defineConfig } from 'vite';
import { fileURLToPath } from 'url'
import solidPlugin from 'vite-plugin-solid';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [solidPlugin()],
  resolve: {
    alias: [
      {
        find: '@websocket',
        replacement: fileURLToPath(new URL('./src/websocket/', import.meta.url)),
      },
      {
        find: '@app',
        replacement: fileURLToPath(new URL('./src/app/', import.meta.url)),
      },
    ],
  },
});
