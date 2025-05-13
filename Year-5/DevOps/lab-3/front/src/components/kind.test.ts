import { expect, test, describe } from "vitest";
import { Section, Operation } from "./kind";

describe("Section Enum", () => {
  test("contains all expected values", () => {
    const expectedValues = [
      "CellItems",
      "Drives",
      "ExportBuses",
      "ImportBuses",
      "Interfaces",
      "Networks",
      "Patterns",
      "StorageCells",
    ];

    const enumKeys = Object.keys(Section)
      .filter((key) => isNaN(Number(key)))
      .sort();

    expect(enumKeys).toEqual(expectedValues.sort());
  });

  test("assigns correct numeric values", () => {
    expect(Section.CellItems).toBe(0);
    expect(Section.Drives).toBe(1);
    expect(Section.ExportBuses).toBe(2);
    expect(Section.ImportBuses).toBe(3);
    expect(Section.Interfaces).toBe(4);
    expect(Section.Networks).toBe(5);
    expect(Section.Patterns).toBe(6);
    expect(Section.StorageCells).toBe(7);
  });

  test("has the correct number of members", () => {
    const enumSize = Object.keys(Section).filter((key) =>
      isNaN(Number(key)),
    ).length;
    expect(enumSize).toBe(8);
  });
});

describe("Operation Enum", () => {
  test("contains all expected values", () => {
    const expectedValues = ["Add", "Update", "Delete"];

    const enumKeys = Object.keys(Operation)
      .filter((key) => isNaN(Number(key)))
      .sort();

    expect(enumKeys).toEqual(expectedValues.sort());
  });

  test("assigns correct numeric values", () => {
    expect(Operation.Add).toBe(0);
    expect(Operation.Update).toBe(1);
    expect(Operation.Delete).toBe(2);
  });

  test("has the correct number of members", () => {
    const enumSize = Object.keys(Operation).filter((key) =>
      isNaN(Number(key)),
    ).length;
    expect(enumSize).toBe(3);
  });
});
