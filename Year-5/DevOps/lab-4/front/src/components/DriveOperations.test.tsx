import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  drives: {
    createDrive: vi.fn().mockResolvedValue(undefined),
    deleteDrive: vi.fn().mockResolvedValue(undefined),
    updateDrive: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();

import { clientGet } from "@state/client";
import { DriveOperations } from "./DriveOperations";
import { Operation } from "./kind";

describe("DriveOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    expect(screen.queryAllByText("Add Drive")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Drive Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Network ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Drive", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Drive")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Drive", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Drive Name")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Drive")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Drive Name")).toBeInTheDocument();
    expect(
      screen.getByText("Update Drive", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Network ID")).not.toBeInTheDocument();
  });

  test("calls addDrive and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Drive Name"), {
      target: { value: "Main Storage" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "1" },
    });

    fireEvent.click(screen.getByText("Add Drive", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.drives.createDrive).toHaveBeenCalledOnce();
      expect(client.drives.createDrive).toHaveBeenCalledWith(
        { networkId: 1 },
        {
          id: undefined,
          driveName: "Main Storage",
          channelsUsed: 0,
          networkId: 1,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        driveName: "Main Storage",
        channelsUsed: 0,
        networkId: 1,
      });
    });
  });

  test("calls deleteDrive and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(screen.getByText("Delete Drive", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.drives.deleteDrive).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateDrive and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Drive Name"), {
      target: { value: "Backup Storage" },
    });

    fireEvent.click(screen.getByText("Update Drive", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.drives.updateDrive).toHaveBeenCalledWith(1, {
        id: 1,
        driveName: "Backup Storage",
        channelsUsed: 0,
        networkId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        driveName: "Backup Storage",
        channelsUsed: 0,
        networkId: 0,
      });
    });
  });

  test("handles empty Network ID input in addDrive", async () => {
    const client = clientGet();
    render(() => <DriveOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Drive Name"), {
      target: { value: "Temporary Drive" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Add Drive", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.drives.createDrive).toHaveBeenCalledWith(
        { networkId: 0 },
        {
          id: undefined,
          driveName: "Temporary Drive",
          channelsUsed: 0,
          networkId: 0,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        id: undefined,
        driveName: "Temporary Drive",
        channelsUsed: 0,
        networkId: 0,
      });
    });
  });
});
