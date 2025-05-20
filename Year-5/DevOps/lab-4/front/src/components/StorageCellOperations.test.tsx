import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  storageCells: {
    createStorageCell: vi.fn().mockResolvedValue(undefined),
    deleteStorageCell: vi.fn().mockResolvedValue(undefined),
    updateStorageCell: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();

import { clientGet } from "@state/client";
import { StorageCellOperations } from "./StorageCellOperations";
import { Operation } from "./kind";

describe("StorageCellOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    expect(screen.queryAllByText("Add Storage Cell")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Capacity")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Drive ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Storage Cell", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getAllByRole("combobox").at(0)!, {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Storage Cell")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Storage Cell", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Capacity")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getAllByRole("combobox").at(0)!, {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Storage Cell")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Capacity")).toBeInTheDocument();
    expect(
      screen.getByText("Update Storage Cell", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Drive ID")).not.toBeInTheDocument();
  });

  test("calls addStorageCell and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Capacity"), {
      target: { value: "1000" },
    });
    fireEvent.change(screen.getByPlaceholderText("Drive ID"), {
      target: { value: "200" },
    });

    fireEvent.click(
      screen.getByText("Add Storage Cell", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.storageCells.createStorageCell).toHaveBeenCalledOnce();
      expect(client.storageCells.createStorageCell).toHaveBeenCalledWith(
        { driveId: 200 },
        {
          id: undefined,
          capacity: 1000,
          driveId: 200,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        capacity: 1000,
        driveId: 200,
      });
    });
  });

  test("calls deleteStorageCell and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getAllByRole("combobox").at(0)!, {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "15" },
    });

    fireEvent.click(
      screen.getByText("Delete Storage Cell", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.storageCells.deleteStorageCell).toHaveBeenCalledWith(15);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 15 });
    });
  });

  test("calls updateStorageCell and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getAllByRole("combobox").at(0)!, {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "3" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Capacity"), {
      target: { value: "2000" },
    });

    fireEvent.click(
      screen.getByText("Update Storage Cell", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.storageCells.updateStorageCell).toHaveBeenCalledWith(3, {
        id: 3,
        capacity: 2000,
        driveId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 3,
        capacity: 2000,
        driveId: 0,
      });
    });
  });

  test("handles empty inputs in addStorageCell", async () => {
    const client = clientGet();
    render(() => <StorageCellOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Capacity"), {
      target: { value: "500" },
    });
    fireEvent.change(screen.getByPlaceholderText("Drive ID"), {
      target: { value: "" },
    });

    fireEvent.click(
      screen.getByText("Add Storage Cell", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.storageCells.createStorageCell).toHaveBeenCalledWith(
        { driveId: 0 },
        {
          id: undefined,
          capacity: 500,
          driveId: 0,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        capacity: 500,
        driveId: 0,
      });
    });
  });
});
