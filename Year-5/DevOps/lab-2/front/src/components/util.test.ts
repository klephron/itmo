import { expect, test, describe } from "vitest";
import { hasString } from "./util"; // Adjust the import path to where hasString is defined

describe("hasString", () => {
  test("returns false for undefined", () => {
    expect(hasString(undefined)).toBe(false);
  });

  test("returns false for empty string", () => {
    expect(hasString("")).toBe(false);
  });

  test("returns true for non-empty string", () => {
    expect(hasString("hello")).toBe(true);
  });

  test("returns true for string with spaces", () => {
    expect(hasString(" ")).toBe(true);
  });

  test("returns true for string with numbers", () => {
    expect(hasString("123")).toBe(true);
  });

  test("returns true for single character", () => {
    expect(hasString("a")).toBe(true);
  });
});
