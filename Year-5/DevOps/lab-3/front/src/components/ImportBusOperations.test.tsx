import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  importBuses: {
    createImportBus: vi.fn().mockResolvedValue(undefined),
    deleteImportBus: vi.fn().mockResolvedValue(undefined),
    updateImportBus: vi.fn().mockResolvedValue(undefined),
    importItems: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();
const mockOnImport = vi.fn();

import { clientGet } from "@state/client";
import { ImportBusOperations } from "./ImportBusOperations";
import { Operation } from "./kind";

describe("ImportBusOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    expect(screen.queryAllByText("Add Import Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Bus Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Network ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Import Bus", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("renders Import Items section", () => {
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    expect(screen.getAllByText("Import Items")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Bus ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Item Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Quantity")).toBeInTheDocument();
    expect(
      screen.getByText("Import Items", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Import Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Import Bus", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Bus Name")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Import Bus")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Bus Name")).toBeInTheDocument();
    expect(
      screen.getByText("Update Import Bus", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addImportBus and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Bus Name"), {
      target: { value: "Main Import Bus" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "1" },
    });

    fireEvent.click(screen.getByText("Add Import Bus", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.importBuses.createImportBus).toHaveBeenCalledOnce();
      expect(client.importBuses.createImportBus).toHaveBeenCalledWith(
        { networkId: 1 },
        {
          id: undefined,
          busName: "Main Import Bus",
          channelsUsed: 0,
          networkId: 1,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        busName: "Main Import Bus",
        channelsUsed: 0,
        networkId: 1,
      });
    });
  });

  test("calls deleteImportBus and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(
      screen.getByText("Delete Import Bus", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.importBuses.deleteImportBus).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateImportBus and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Bus Name"), {
      target: { value: "Updated Import Bus" },
    });

    fireEvent.click(
      screen.getByText("Update Import Bus", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.importBuses.updateImportBus).toHaveBeenCalledWith(1, {
        id: 1,
        busName: "Updated Import Bus",
        channelsUsed: 0,
        networkId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        busName: "Updated Import Bus",
        channelsUsed: 0,
        networkId: 0,
      });
    });
  });

  test("calls importItems and onImport with correct data", async () => {
    const client = clientGet();
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
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

    fireEvent.click(screen.getByText("Import Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.importBuses.importItems).toHaveBeenCalledWith(3, {
        itemName: "Iron Ingot",
        quantity: 64,
      });
      expect(mockOnImport).toHaveBeenCalledWith({ id: 3 });
    });
  });

  test("handles empty quantity in importItems", async () => {
    const client = clientGet();
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
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

    fireEvent.click(screen.getByText("Import Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.importBuses.importItems).toHaveBeenCalledWith(4, {
        itemName: "Copper Wire",
        quantity: 0,
      });
      expect(mockOnImport).toHaveBeenCalledWith({ id: 4 });
    });
  });

  test("shares ID input between operation forms and import section", async () => {
    render(() => (
      <ImportBusOperations onUpdate={mockOnUpdate} onImport={mockOnImport} />
    ));

    // Set ID in operation form
    fireEvent.change(screen.getAllByRole("combobox")[0], {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "10" },
    });

    // Verify ID is shared with import section
    expect(screen.getByPlaceholderText("Bus ID")).toHaveValue(10);

    // Change ID in import section
    fireEvent.change(screen.getByPlaceholderText("Bus ID"), {
      target: { value: "20" },
    });

    // Verify ID is shared with operation form
    expect(screen.getByPlaceholderText("ID to Delete")).toHaveValue(20);
  });
});
