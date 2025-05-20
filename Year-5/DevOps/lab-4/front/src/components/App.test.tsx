import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";
import { App } from "./App";
import { clientGet } from "@state/client";
import {
  CellItemDto,
  DriveDto,
  ExportBusDto,
  ImportBusDto,
  MeInterfaceDto,
  NetworkDto,
  PatternDto,
  StorageCellDto,
} from "@api/generated/data-contracts";

const mockCellItems: CellItemDto[] = [
  {
    id: 1,
    itemName: "Iron Ingot",
    quantity: 64,
    storageCellId: 100,
  },
];

const mockDrives: DriveDto[] = [
  {
    id: 1,
    driveName: "Main Drive",
    channelsUsed: 8,
    networkId: 1,
  },
];

const mockExportBuses: ExportBusDto[] = [
  {
    id: 1,
    busName: "Iron Export",
    channelsUsed: 2,
    networkId: 1,
  },
];

const mockImportBuses: ImportBusDto[] = [
  {
    id: 1,
    busName: "Gold Import",
    channelsUsed: 4,
    networkId: 1,
  },
];

const mockInterfaces: MeInterfaceDto[] = [
  {
    id: 1,
    interfaceName: "Main Interface",
    channelsUsed: 6,
    networkId: 1,
  },
];

const mockNetworks: NetworkDto[] = [
  {
    id: 1,
    name: "Main Network",
    channelCapacity: 32,
    usedChannels: 20,
  },
];

const mockPatterns: PatternDto[] = [
  {
    id: 1,
    patternName: "Iron Sword",
    inputItems: "Iron Ingot, Stick",
    outputItem: "Iron Sword",
    outputQuantity: 1,
    meInterfaceId: 1,
  },
];

const mockStorageCells: StorageCellDto[] = [
  {
    id: 1,
    capacity: 1000,
    driveId: 1,
  },
];

const mockClient = {
  cellItems: {
    getAllCellItems: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockCellItems),
    }),
  },
  drives: {
    getAllDrives: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockDrives),
    }),
  },
  exportBuses: {
    getAllExportBuses: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockExportBuses),
    }),
  },
  importBuses: {
    getAllImportBuses: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockImportBuses),
    }),
  },
  interfaces: {
    getAllInterfaces: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockInterfaces),
    }),
  },
  networks: {
    getAllNetworks: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockNetworks),
    }),
  },
  patterns: {
    getAllPatterns: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockPatterns),
    }),
  },
  storageCells: {
    getAllStorageCells: vi.fn().mockResolvedValue({
      json: () => Promise.resolve(mockStorageCells),
    }),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

describe("App", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders sidebar with all navigation items", () => {
    render(() => <App />);

    expect(screen.getByText("Cell Items")).toBeInTheDocument();
    expect(screen.getByText("Drives")).toBeInTheDocument();
    expect(screen.getByText("Export Buses")).toBeInTheDocument();
    expect(screen.getByText("Import Buses")).toBeInTheDocument();
    expect(screen.getByText("ME Interfaces")).toBeInTheDocument();
    expect(screen.getByText("Networks")).toBeInTheDocument();
    expect(screen.getByText("Patterns")).toBeInTheDocument();
    expect(screen.getByText("Storage Cells")).toBeInTheDocument();
  });

  test("fetches CellItems on mount and renders CellItemTable and CellItemOperations", async () => {
    const client = clientGet();
    render(() => <App />);

    await vi.waitFor(() => {
      expect(client.cellItems.getAllCellItems).toHaveBeenCalledOnce();
      expect(screen.getByText("Iron Ingot")).toBeInTheDocument();
      expect(screen.getAllByText("Add Cell Item")).length.greaterThanOrEqual(1);
    });
  });

  test("switches to Drives section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Drives"));

    await vi.waitFor(() => {
      expect(client.drives.getAllDrives).toHaveBeenCalledOnce();
      expect(screen.getByText("Main Drive")).toBeInTheDocument();
      expect(screen.getAllByText("Add Drive")).length.greaterThanOrEqual(1);
    });
  });

  test("switches to ExportBuses section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Export Buses"));

    await vi.waitFor(() => {
      expect(client.exportBuses.getAllExportBuses).toHaveBeenCalledOnce();
      expect(screen.getByText("Iron Export")).toBeInTheDocument();
      expect(screen.getAllByText("Add Export Bus")).length.greaterThanOrEqual(
        1,
      );
    });
  });

  test("switches to ImportBuses section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Import Buses"));

    await vi.waitFor(() => {
      expect(client.importBuses.getAllImportBuses).toHaveBeenCalledOnce();
      expect(screen.getByText("Gold Import")).toBeInTheDocument();
      expect(screen.getAllByText("Add Import Bus")).length.greaterThanOrEqual(
        1,
      );
    });
  });

  test("switches to Interfaces section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("ME Interfaces"));

    await vi.waitFor(() => {
      expect(client.interfaces.getAllInterfaces).toHaveBeenCalledOnce();
      expect(screen.getByText("Main Interface")).toBeInTheDocument();
      expect(screen.getAllByText("Add Interface")).length.greaterThanOrEqual(1);
    });
  });

  test("switches to Networks section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Networks"));

    await vi.waitFor(() => {
      expect(client.networks.getAllNetworks).toHaveBeenCalledOnce();
      expect(screen.getByText("Main Network")).toBeInTheDocument();
      expect(screen.getAllByText("Add Network")).length.greaterThanOrEqual(1);
    });
  });

  test("switches to Patterns section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Patterns"));

    await vi.waitFor(() => {
      expect(client.patterns.getAllPatterns).toHaveBeenCalledOnce();
      expect(screen.getAllByText("Iron Sword")).length.greaterThanOrEqual(1);
      expect(screen.getAllByText("Add Pattern")).length.greaterThanOrEqual(1);
    });
  });

  test("switches to StorageCells section and fetches data", async () => {
    const client = clientGet();
    render(() => <App />);

    fireEvent.click(screen.getByText("Storage Cells"));

    await vi.waitFor(() => {
      expect(client.storageCells.getAllStorageCells).toHaveBeenCalledOnce();
      expect(screen.getAllByText("Add Storage Cell")).length.greaterThanOrEqual(
        1,
      );
    });
  });
});
