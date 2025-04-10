import { expect, test, describe, afterEach } from "vitest";

import { render, screen, cleanup } from "@solidjs/testing-library";
import { CellItemTable } from "./CellItemTable"; // Adjust the import path
import { CellItemEntityDto } from "@api/generated/data-contracts";

describe("CellItemTable", () => {
  const mockItems: CellItemEntityDto[] = [
    {
      id: 1,
      itemName: "Iron Ingot",
      quantity: 64,
      storageCellId: 100,
    },
    {
      id: 2,
      itemName: "Gold Ingot",
      quantity: 32,
      storageCellId: 101,
    },
  ];

  afterEach(() => {
    cleanup();
  });

  test("renders table headers correctly", () => {
    render(() => <CellItemTable items={[]} />);
    expect(screen.getByText("ID")).toBeInTheDocument();
    expect(screen.getByText("Item Name")).toBeInTheDocument();
    expect(screen.getByText("Quantity")).toBeInTheDocument();
    expect(screen.getByText("Storage Cell ID")).toBeInTheDocument();
  });

  test("renders correct number of rows for multiple items", () => {
    render(() => <CellItemTable items={mockItems} />);

    const rows = screen.getAllByRole("row");
    expect(rows.length).toBe(3);
  });

  test("renders table rows based on items prop", () => {
    render(() => <CellItemTable items={mockItems} />);

    expect(screen.getByText("1")).toBeInTheDocument();
    expect(screen.getByText("Iron Ingot")).toBeInTheDocument();
    expect(screen.getByText("64")).toBeInTheDocument();
    expect(screen.getByText("100")).toBeInTheDocument();

    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("Gold Ingot")).toBeInTheDocument();
    expect(screen.getByText("32")).toBeInTheDocument();
    expect(screen.getByText("101")).toBeInTheDocument();
  });

  test("renders no rows when items array is empty", () => {
    render(() => <CellItemTable items={[]} />);

    const rows = screen.getAllByRole("row");
    expect(rows.length).toBe(1);
    expect(screen.queryByText("Iron Ingot")).not.toBeInTheDocument();
  });
});
