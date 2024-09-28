import { createEffect, createSignal } from "solid-js";

import Map from "./Map.jsx";

function App() {
  const [getTextAreaText, setTextAreaText] = createSignal("");
  const [getMessageText, setMessageText] = createSignal("");

  let websocket = new WebSocket("ws://localhost:5325/ws");
  let timerId = 0;

  // < 30 seconds
  function keepAlive(timeout = 20_000) {
    if (websocket.readyState == websocket.OPEN) {
      websocket.send("ping");
    }
    timerId = setTimeout(keepAlive, timeout);
  }

  function cancelKeepAlive() {
    if (timerId) {
      clearTimeout(timerId);
    }
  }

  websocket.onopen = function () {
    setTextAreaText(getTextAreaText() + "Connected to WebSocket server\n");
    keepAlive();
  };

  websocket.onmessage = function (event) {
    setTextAreaText(
      getTextAreaText() + "Received from server: " + event.data + "\n",
    );
  };

  websocket.onclose = function () {
    setTextAreaText(getTextAreaText() + "WebSocket connection closed\n");
    cancelKeepAlive();
  };

  function sendMessage() {
    websocket.send(getMessageText());
    setTextAreaText(
      getTextAreaText() + "Sent to server: " + getMessageText() + "\n",
    );
    setMessageText("");
  }
  return (
    <>
      <h1>Reactive Java View</h1>
      <Map width={600} height={400}/>
      <br />
      <textarea rows="10" cols="50" value={getTextAreaText()}></textarea>
      <br />
      <input
        placeholder="Enter message"
        value={getMessageText()}
        onChange={(e) => setMessageText(e.target.value)}
      />
      <button onClick={sendMessage}>Send</button>
    </>
  );
}

export default App;
