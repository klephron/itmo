import { createSignal } from "solid-js";

import { CellItems } from "@api/generated/CellItems";
import { Drives } from "@api/generated/Drives";
import { Exportbuses } from "@api/generated/Exportbuses";
import { Importbuses } from "@api/generated/Importbuses";
import { Items } from "@api/generated/Items";
import { Networks } from "@api/generated/Networks";
import { Patterns } from "@api/generated/Patterns";
import { StorageCells } from "@api/generated/StorageCells";
import { Interfaces } from "@api/generated/Interfaces";

export class Client {
  cellItems: CellItems;
  drives: Drives;
  exportBuses: Exportbuses;
  importBuses: Importbuses;
  interfaces: Interfaces;
  items: Items;
  networks: Networks;
  patterns: Patterns;
  storageCells: StorageCells;

  constructor(baseUrl?: string) {
    this.cellItems = new CellItems({ baseUrl: baseUrl });
    this.drives = new Drives({ baseUrl: baseUrl });
    this.exportBuses = new Exportbuses({ baseUrl: baseUrl });
    this.importBuses = new Importbuses({ baseUrl: baseUrl });
    this.interfaces = new Interfaces({ baseUrl: baseUrl });
    this.items = new Items({ baseUrl: baseUrl });
    this.networks = new Networks({ baseUrl: baseUrl });
    this.patterns = new Patterns({ baseUrl: baseUrl });
    this.storageCells = new StorageCells({ baseUrl: baseUrl });
  }
}

export const [clientGet, clientSet] = createSignal<Client>(
  new Client(import.meta.env.VITE_SERVER_URL),
);
