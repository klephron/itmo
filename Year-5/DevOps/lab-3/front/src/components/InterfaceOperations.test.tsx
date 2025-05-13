import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  interfaces: {
    createInterface: vi.fn().mockResolvedValue(undefined),
    deleteInterface: vi.fn().mockResolvedValue(undefined),
    updateInterface: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();

import { clientGet } from "@state/client";
import { InterfaceOperations } from "./InterfaceOperations";
import { Operation } from "./kind";

describe("InterfaceOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    expect(screen.queryAllByText("Add Interface")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Interface Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Network ID")).toBeInTheDocument();
    expect(
      screen.getByText("Add Interface", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Interface")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Interface", { selector: "button" }),
    ).toBeInTheDocument();
    expect(
      screen.queryByPlaceholderText("Interface Name"),
    ).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Interface")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New Interface Name"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Update Interface", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addInterface and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Interface Name"), {
      target: { value: "Ethernet0" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "1" },
    });

    fireEvent.click(screen.getByText("Add Interface", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.interfaces.createInterface).toHaveBeenCalledOnce();
      expect(client.interfaces.createInterface).toHaveBeenCalledWith(
        { networkId: 1 },
        {
          interfaceName: "Ethernet0",
          networkId: 1,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        interfaceName: "Ethernet0",
        networkId: 1,
      });
    });
  });

  test("calls deleteInterface and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(
      screen.getByText("Delete Interface", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.interfaces.deleteInterface).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateInterface and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Interface Name"), {
      target: { value: "GigabitEthernet0/1" },
    });

    fireEvent.click(
      screen.getByText("Update Interface", { selector: "button" }),
    );

    await vi.waitFor(() => {
      expect(client.interfaces.updateInterface).toHaveBeenCalledWith(1, {
        id: 1,
        interfaceName: "GigabitEthernet0/1",
        networkId: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        interfaceName: "GigabitEthernet0/1",
        networkId: 0,
      });
    });
  });

  test("handles empty Network ID in addInterface", async () => {
    const client = clientGet();
    render(() => <InterfaceOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Interface Name"), {
      target: { value: "Loopback0" },
    });
    fireEvent.change(screen.getByPlaceholderText("Network ID"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Add Interface", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.interfaces.createInterface).toHaveBeenCalledWith(
        { networkId: 0 },
        {
          interfaceName: "Loopback0",
          networkId: 0,
        },
      );
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        interfaceName: "Loopback0",
        networkId: 0,
      });
    });
  });
});
