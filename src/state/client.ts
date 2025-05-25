import { Client } from "@api/client";
import { create } from "zustand";
import { combine } from "zustand/middleware";

const useClient = create(
  combine({ client: new Client(import.meta.env.VITE_SERVER_URL) }, (set) => ({
    update: (baseUrl: string) => set({ client: new Client(baseUrl) }),
  }))
);

export { useClient };
