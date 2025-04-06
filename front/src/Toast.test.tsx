import { test, expect } from "vitest";
import { render, screen } from "@solidjs/testing-library";
import { Toast } from "./Toast";

test("increments value", async () => {
  render(() => (
    <Toast>
      <p>This is a toast</p>
    </Toast>
  ));
  const toast = screen.getByRole("log");
  expect(toast).toHaveTextContent("This is a toast");
});
