import { createSignal } from "solid-js";

export const Counter = () => {
  console.log(import.meta.env.VITE_SERVER_URL)
  const [count, setCount] = createSignal(0);
  return <button onClick={() => setCount(count() + 1)}>{count()}</button>;
};
