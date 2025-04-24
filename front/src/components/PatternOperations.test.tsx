import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  patterns: {
    createPattern: vi.fn().mockResolvedValue(undefined),
    deletePattern: vi.fn().mockResolvedValue(undefined),
    updatePattern: vi.fn().mockResolvedValue(undefined),
    craftPattern: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();
const mockOnCraft = vi.fn();

import { clientGet } from "@state/client";
import { PatternOperations } from "./PatternOperations";
import { Operation } from "./kind";

describe("PatternOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    expect(screen.queryAllByText("Add Pattern")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Pattern Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Input Items")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Output Item Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Output Quantity")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("ME Interface ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Pattern", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("renders Craft Items form", () => {
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    expect(screen.getAllByText("Craft Items")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Pattern ID")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Craft Times")).toBeInTheDocument();
    expect(
      screen.getByText("Craft Items", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getAllByRole("combobox", { name: "" }).at(0)!, {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Pattern")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Pattern", { selector: "button" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByPlaceholderText("Pattern Name"),
    ).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getAllByRole("combobox", { name: "" }).at(0)!, {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Pattern")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Pattern Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Input Items")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New Output Item Name"),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New Output Quantity"),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New ME Interface ID"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Update Pattern", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addPattern and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Pattern Name"), {
      target: { value: "Iron Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("Input Items"), {
      target: { value: "Iron Ingot, Stick" },
    });
    fireEvent.change(screen.getByPlaceholderText("Output Item Name"), {
      target: { value: "Iron Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("Output Quantity"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("ME Interface ID"), {
      target: { value: "50" },
    });

    fireEvent.click(screen.getByText("Add Pattern", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.createPattern).toHaveBeenCalledOnce();
      expect(client.patterns.createPattern).toHaveBeenCalledWith({
        id: undefined,
        patternName: "Iron Sword",
        inputItems: "Iron Ingot, Stick",
        outputItem: "Iron Sword",
        outputQuantity: 1,
        meInterfaceId: 50,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        patternName: "Iron Sword",
        inputItems: "Iron Ingot, Stick",
        outputItem: "Iron Sword",
        outputQuantity: 1,
        meInterfaceId: 50,
      });
    });
  });

  test("calls deletePattern and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getAllByRole("combobox", { name: "" }).at(0)!, {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "10" },
    });

    fireEvent.click(screen.getByText("Delete Pattern", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.deletePattern).toHaveBeenCalledWith(10);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 10 });
    });
  });

  test("calls updatePattern and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getAllByRole("combobox", { name: "" }).at(0)!, {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "2" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Pattern Name"), {
      target: { value: "Gold Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Input Items"), {
      target: { value: "Gold Ingot, Stick" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Output Item Name"), {
      target: { value: "Gold Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Output Quantity"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New ME Interface ID"), {
      target: { value: "60" },
    });

    fireEvent.click(screen.getByText("Update Pattern", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.updatePattern).toHaveBeenCalledWith(2, {
        id: 2,
        patternName: "Gold Sword",
        inputItems: "Gold Ingot, Stick",
        outputItem: "Gold Sword",
        outputQuantity: 1,
        meInterfaceId: 60,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 2,
        patternName: "Gold Sword",
        inputItems: "Gold Ingot, Stick",
        outputItem: "Gold Sword",
        outputQuantity: 1,
        meInterfaceId: 60,
      });
    });
  });

  test("calls craftPattern and onCraft with correct data", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Pattern ID"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByPlaceholderText("Craft Times"), {
      target: { value: "5" },
    });

    fireEvent.click(screen.getByText("Craft Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.craftPattern).toHaveBeenCalledWith(3, {
        times: 5,
      });
      expect(mockOnCraft).toHaveBeenCalledWith({ id: 3 });
    });
  });

  test("handles empty inputs in addPattern", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Pattern Name"), {
      target: { value: "Diamond Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("Input Items"), {
      target: { value: "Diamond, Stick" },
    });
    fireEvent.change(screen.getByPlaceholderText("Output Item Name"), {
      target: { value: "Diamond Sword" },
    });
    fireEvent.change(screen.getByPlaceholderText("Output Quantity"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("ME Interface ID"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Add Pattern", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.createPattern).toHaveBeenCalledWith({
        id: undefined,
        patternName: "Diamond Sword",
        inputItems: "Diamond, Stick",
        outputItem: "Diamond Sword",
        outputQuantity: 1,
        meInterfaceId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        patternName: "Diamond Sword",
        inputItems: "Diamond, Stick",
        outputItem: "Diamond Sword",
        outputQuantity: 1,
        meInterfaceId: 0,
      });
    });
  });

  test("handles empty craft times in craftPattern", async () => {
    const client = clientGet();
    render(() => (
      <PatternOperations onUpdate={mockOnUpdate} onCraft={mockOnCraft} />
    ));

    fireEvent.change(screen.getByPlaceholderText("Pattern ID"), {
      target: { value: "4" },
    });
    fireEvent.change(screen.getByPlaceholderText("Craft Times"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Craft Items", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.patterns.craftPattern).toHaveBeenCalledWith(4, {
        times: 0,
      });
      expect(mockOnCraft).toHaveBeenCalledWith({ id: 4 });
    });
  });
});
