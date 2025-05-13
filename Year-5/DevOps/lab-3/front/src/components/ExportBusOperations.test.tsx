import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  exportBuses: {
    createExportBus: vi.fn().mockResolvedValue(undefined),
    deleteExportBus: vi.fn().mockResolvedValue(undefined),
    updateExportBus: vi.fn().mockResolvedValue(undefined),
    exportItems: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();
const mockOnExport = vi.fn();

import { clientGet } from "@state/client";
import { ExportBusOperations } from "./ExportBusOperations";
import { Operation } from "./kind";

describe("ExportBusOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    expect(screen.queryAllByText("Add Export Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Bus Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Network ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Export Bus", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("renders Export Items section", () => {
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    expect(screen.getAllByText("Export Items")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Bus ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Item Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Quantity")).toBeInTheDocument();
    expect(
      screen.getByText("Export Items", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Export Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Export Bus", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Bus Name")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Export Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Bus Name")).toBeInTheDocument();
    expect(
      screen.getByText("Update Export Bus", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addExportBus and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Bus Name"), {
      target: { value: "Main Export Bus" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "1" },
    });

    fireEvent.click(screen.getByText("Add Export Bus", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.exportBuses.createExportBus).toHaveBeenCalledOnce();
      expect(client.exportBuses.createExportBus).toHaveBeenCalledWith(
        { networkId: 1 },
        {
          id: undefined,
          busName: "Main Export Bus",
          channelsUsed: 0,
          networkId: 1,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        busName: "Main Export Bus",
        channelsUsed: 0,
        networkId: 1,
      });
    });
  });

  test("calls deleteExportBus and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(
      screen.getByText("Delete Export Bus", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.exportBuses.deleteExportBus).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateExportBus and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Bus Name"), {
      target: { value: "Updated Export Bus" },
    });

    fireEvent.click(
      screen.getByText("Update Export Bus", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.exportBuses.updateExportBus).toHaveBeenCalledWith(1, {
        id: 1,
        busName: "Updated Export Bus",
        channelsUsed: 0,
        networkId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        busName: "Updated Export Bus",
        channelsUsed: 0,
        networkId: 0,
      });
    });
  });

  test("calls exportItems and onExport with correct data", async () => {
    const client = clientGet();
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Bus ID"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByPlaceholderText("Item Name"), {
      target: { value: "Iron Ingot" },
    });
    fireEvent.change(screen.getByPlaceholderText("Quantity"), {
      target: { value: "64" },
    });

    fireEvent.click(screen.getByText("Export Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.exportBuses.exportItems).toHaveBeenCalledWith(3, {
        itemName: "Iron Ingot",
        quantity: 64,
      });
      expect(mockOnExport).toHaveBeenCalledWith({ id: 3 });
    });
  });

  test("handles empty quantity in exportItems", async () => {
    const client = clientGet();
    render(() => (
      <ExportBusOperations onUpdate={mockOnUpdate} onExport={mockOnExport} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Bus ID"), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByPlaceholderText("Item Name"), {
      target: { value: "Copper Wire" },
    });
    fireEvent.change(screen.getByPlaceholderText("Quantity"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Export Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.exportBuses.exportItems).toHaveBeenCalledWith(4, {
        itemName: "Copper Wire",
        quantity: 0,
      });
      expect(mockOnExport).toHaveBeenCalledWith({ id: 4 });
    });
  });
});
