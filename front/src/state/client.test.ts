import { expect, test, describe, vi, afterEach } from "vitest";
import { cleanup } from "@solidjs/testing-library";

vi.mock("@api/generated/CellItems", () => ({
  CellItems: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Drives", () => ({
  Drives: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Exportbuses", () => ({
  Exportbuses: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Importbuses", () => ({
  Importbuses: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Interfaces", () => ({
  Interfaces: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Items", () => ({
  Items: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Networks", () => ({
  Networks: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/Patterns", () => ({
  Patterns: vi.fn().mockImplementation(() => ({})),
}));
vi.mock("@api/generated/StorageCells", () => ({
  StorageCells: vi.fn().mockImplementation(() => ({})),
}));

import { Client, clientGet, clientSet } from "./client";
import { CellItems } from "@api/generated/CellItems";
import { Drives } from "@api/generated/Drives";
import { Exportbuses } from "@api/generated/Exportbuses";
import { Importbuses } from "@api/generated/Importbuses";
import { Interfaces } from "@api/generated/Interfaces";
import { Items } from "@api/generated/Items";
import { Networks } from "@api/generated/Networks";
import { Patterns } from "@api/generated/Patterns";
import { StorageCells } from "@api/generated/StorageCells";

describe("Client and Signals", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("Client initializes with correct baseUrl", () => {
    const baseUrl = "http://test-server:8080";
    const client = new Client(baseUrl);

    expect(CellItems).toHaveBeenCalledWith({ baseUrl });
    expect(Drives).toHaveBeenCalledWith({ baseUrl });
    expect(Exportbuses).toHaveBeenCalledWith({ baseUrl });
    expect(Importbuses).toHaveBeenCalledWith({ baseUrl });
    expect(Interfaces).toHaveBeenCalledWith({ baseUrl });
    expect(Items).toHaveBeenCalledWith({ baseUrl });
    expect(Networks).toHaveBeenCalledWith({ baseUrl });
    expect(Patterns).toHaveBeenCalledWith({ baseUrl });
    expect(StorageCells).toHaveBeenCalledWith({ baseUrl });

    expect(client.cellItems).toBeDefined();
    expect(client.drives).toBeDefined();
    expect(client.exportBuses).toBeDefined();
    expect(client.importBuses).toBeDefined();
    expect(client.interfaces).toBeDefined();
    expect(client.items).toBeDefined();
    expect(client.networks).toBeDefined();
    expect(client.patterns).toBeDefined();
    expect(client.storageCells).toBeDefined();
  });

  test("clientSet updates the client instance", () => {
    const newClient = new Client("http://new-server:9000");

    clientSet(newClient);

    expect(clientGet()).toBe(newClient);
    expect(CellItems).toHaveBeenCalledWith({
      baseUrl: "http://new-server:9000",
    });
  });
});
