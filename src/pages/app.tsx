import { useEffect, useState } from "react";
import reactLogo from "@assets/react.svg";
import viteLogo from "/vite.svg";
import "./app.scss";
import { useClient } from "@state/client";

function App() {
  const [count, setCount] = useState(0);
  const client = useClient((state) => state.client);

  // useEffect(() => {
  //   client.test.register({
  //     username: "string",
  //     password: "string",
  //     name: "string",
  //     surname: "string",
  //     patronymic: "string",
  //   });
  // });

  return (
    <>
      <div>
        <a href="https://vite.dev" target="_blank">
          <img src={viteLogo} className="logo" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank">
          <img src={reactLogo} className="logo react" alt="React logo" />
        </a>
      </div>
      <h1>Vite + React</h1>
      <div className="card">
        <button onClick={() => setCount((count) => count + 1)}>count is {count}</button>
        <p>
          Edit <code>src/App.tsx</code> and save to test HMR
        </p>
        <p>SERVER_URL: {import.meta.env.VITE_SERVER_URL}</p>
      </div>
      <p className="read-the-docs">Click on the Vite and React logos to learn more</p>
    </>
  );
}

export default App;
