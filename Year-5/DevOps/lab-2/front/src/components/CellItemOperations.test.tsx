import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  cellItems: {
    createCellItem: vi.fn().mockResolvedValue(undefined),
    deleteCellItem: vi.fn().mockResolvedValue(undefined),
    updateCellItem: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();

import { clientGet } from "@state/client";
import { CellItemOperations } from "./CellItemOperations";
import { Operation } from "./kind";

describe("CellItemOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    expect(screen.queryAllByText("Add Cell Item")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Item Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Quantity")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Storage Cell ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Cell Item", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Cell Item")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Cell Item", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Item Name")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Cell Item")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Item Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Quantity")).toBeInTheDocument();
    expect(
      screen.getByText("Update Cell Item", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addCellItem and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Item Name"), {
      target: { value: "Iron Ingot" },
    });
    fireEvent.change(screen.getByPlaceholderText("Quantity"), {
      target: { value: "64" },
    });
    fireEvent.change(screen.getByPlaceholderText("Storage Cell ID"), {
      target: { value: "100" },
    });

    fireEvent.click(screen.getByText("Add Cell Item", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.cellItems.createCellItem).toHaveBeenCalledOnce();
      expect(client.cellItems.createCellItem).toHaveBeenCalledWith(
        { storageCellId: 100 },
        {
          id: undefined,
          itemName: "Iron Ingot",
          quantity: 64,
          storageCellId: 100,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        itemName: "Iron Ingot",
        quantity: 64,
        storageCellId: 100,
      });
    });
  });

  test("calls deleteCellItem and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(
      screen.getByText("Delete Cell Item", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.cellItems.deleteCellItem).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateCellItem and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Item Name"), {
      target: { value: "Gold Ingot" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Quantity"), {
      target: { value: "32" },
    });

    fireEvent.click(
      screen.getByText("Update Cell Item", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.cellItems.updateCellItem).toHaveBeenCalledWith(1, {
        id: 1,
        itemName: "Gold Ingot",
        quantity: 32,
        storageCellId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        itemName: "Gold Ingot",
        quantity: 32,
        storageCellId: 0,
      });
    });
  });

  test("handles empty ID input in addCellItem", async () => {
    const client = clientGet();
    render(() => <CellItemOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Item Name"), {
      target: { value: "Copper" },
    });
    fireEvent.change(screen.getByPlaceholderText("Quantity"), {
      target: { value: "10" },
    });
    fireEvent.change(screen.getByPlaceholderText("Storage Cell ID"), {
      target: { value: "200" },
    });
    fireEvent.change(screen.getByPlaceholderText("Storage Cell ID"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Add Cell Item", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.cellItems.createCellItem).toHaveBeenCalledWith(
        { storageCellId: 0 },
        {
          id: undefined,
          itemName: "Copper",
          quantity: 10,
          storageCellId: 0,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        itemName: "Copper",
        quantity: 10,
        storageCellId: 0,
      });
    });
  });
});
