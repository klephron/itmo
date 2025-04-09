import { render } from "solid-js/web";

import "./index.scss";
import { Counter } from "./Counter";

const root = document.getElementById("root");

if (root !== null) {
  render(() => <Counter />, root!);
}
