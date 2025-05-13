import { expect, test, describe, vi, afterEach } from "vitest";
import { render, screen, fireEvent, cleanup } from "@solidjs/testing-library";

const mockClient = {
  networks: {
    createNetwork: vi.fn().mockResolvedValue(undefined),
    deleteNetwork: vi.fn().mockResolvedValue(undefined),
    updateNetwork: vi.fn().mockResolvedValue(undefined),
  },
};

vi.mock("@state/client", () => {
  return {
    clientGet: () => mockClient,
  };
});

const mockOnUpdate = vi.fn();

import { clientGet } from "@state/client";
import { NetworkOperations } from "./NetworkOperations";
import { Operation } from "./kind";

describe("NetworkOperations", () => {
  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  test("renders Add operation form by default", () => {
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    expect(screen.queryAllByText("Add Network")).toHaveLength(2);
    expect(screen.getByPlaceholderText("Name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Channel Capacity")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Used Channels")).toBeInTheDocument();
    expect(
      screen.getByText("Add Network", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("switches to Delete operation form when selected", () => {
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });

    expect(screen.queryAllByText("Delete Network")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Delete")).toBeInTheDocument();
    expect(
      screen.getByText("Delete Network", { selector: "button" }),
    ).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("Name")).not.toBeInTheDocument();
  });

  test("switches to Update operation form when selected", () => {
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });

    expect(screen.queryAllByText("Update Network")).toHaveLength(2);
    expect(screen.getByPlaceholderText("ID to Update")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("New Name")).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New Channel Capacity"),
    ).toBeInTheDocument();
    expect(
      screen.getByPlaceholderText("New Used Channels"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("Update Network", { selector: "button" }),
    ).toBeInTheDocument();
  });

  test("calls addNetwork and onUpdate with correct data", async () => {
    const client = clientGet();

    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Name"), {
      target: { value: "Main Network" },
    });
    fireEvent.change(screen.getByPlaceholderText("Channel Capacity"), {
      target: { value: "32" },
    });
    fireEvent.change(screen.getByPlaceholderText("Used Channels"), {
      target: { value: "8" },
    });

    fireEvent.click(screen.getByText("Add Network", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.networks.createNetwork).toHaveBeenCalledOnce();
      expect(client.networks.createNetwork).toHaveBeenCalledWith({
        name: "Main Network",
        channelCapacity: 32,
        usedChannels: 8,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        name: "Main Network",
        channelCapacity: 32,
        usedChannels: 8,
      });
    });
  });

  test("calls deleteNetwork and onUpdate with correct ID", async () => {
    const client = clientGet();
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Delete },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Delete"), {
      target: { value: "5" },
    });

    fireEvent.click(screen.getByText("Delete Network", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.networks.deleteNetwork).toHaveBeenCalledWith(5);
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Delete, { id: 5 });
    });
  });

  test("calls updateNetwork and onUpdate with correct data", async () => {
    const client = clientGet();
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: Operation.Update },
    });
    fireEvent.change(screen.getByPlaceholderText("ID to Update"), {
      target: { value: "1" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Name"), {
      target: { value: "Updated Network" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Channel Capacity"), {
      target: { value: "64" },
    });
    fireEvent.change(screen.getByPlaceholderText("New Used Channels"), {
      target: { value: "16" },
    });

    fireEvent.click(screen.getByText("Update Network", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.networks.updateNetwork).toHaveBeenCalledWith(1, {
        id: 1,
        name: "Updated Network",
        channelCapacity: 64,
        usedChannels: 16,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Update, {
        id: 1,
        name: "Updated Network",
        channelCapacity: 64,
        usedChannels: 16,
      });
    });
  });

  test("handles empty numeric inputs in addNetwork", async () => {
    const client = clientGet();
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Name"), {
      target: { value: "Test Network" },
    });
    fireEvent.change(screen.getByPlaceholderText("Channel Capacity"), {
      target: { value: "" },
    });
    fireEvent.change(screen.getByPlaceholderText("Used Channels"), {
      target: { value: "" },
    });

    fireEvent.click(screen.getByText("Add Network", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.networks.createNetwork).toHaveBeenCalledWith({
        name: "Test Network",
        channelCapacity: 0,
        usedChannels: 0,
      });
      expect(mockOnUpdate).toHaveBeenCalledWith(Operation.Add, {
        name: "Test Network",
        channelCapacity: 0,
        usedChannels: 0,
      });
    });
  });

  test("handles minimum values for numeric inputs", async () => {
    const client = clientGet();
    render(() => <NetworkOperations onUpdate={mockOnUpdate} />);

    fireEvent.change(screen.getByPlaceholderText("Name"), {
      target: { value: "Min Network" },
    });
    fireEvent.change(screen.getByPlaceholderText("Channel Capacity"), {
      target: { value: "-5" },
    });
    fireEvent.change(screen.getByPlaceholderText("Used Channels"), {
      target: { value: "-10" },
    });

    fireEvent.click(screen.getByText("Add Network", { selector: "button" }));

    await vi.waitFor(() => {
      expect(client.networks.createNetwork).toHaveBeenCalledWith({
        name: "Min Network",
        channelCapacity: -5,
        usedChannels: -10,
      });
    });
  });
});
