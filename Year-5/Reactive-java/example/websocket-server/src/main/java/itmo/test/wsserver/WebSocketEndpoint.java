package itmo.test.wsserver;

import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ServerEndpoint("")
public class WebSocketEndpoint {

  @OnOpen
  public void onOpen(Session session) {
    System.out.println("WebSocket opened " + session.getId());
  }

  @OnMessage
  public void onMessage(String message, Session session) {
    String text;

    System.out.println("WebSocket from " + session.getId() + " - " + message);

    if (message.equals("ping")) {
      text = "pong";
    } else {
      text = "Echo: " + message;
    }
    try {
      session.getBasicRemote().sendText(text);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @OnClose
  public void onClose(Session session) {
    System.out.println("WebSocket closed " + session.getId());
  }
}
