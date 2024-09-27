package itmo.test.wsserver;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import jakarta.websocket.server.ServerEndpointConfig;

public class App {
  public static void main(String[] args) throws Exception {
    int port = 5325;
    Server server = new Server(port);

    ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    handler.setContextPath("/");

    JakartaWebSocketServletContainerInitializer.configure(handler, (context, container) -> {
      // Add echo endpoint to server container
      ServerEndpointConfig echoConfig = ServerEndpointConfig.Builder.create(WebSocketEndpoint.class, "/ws").build();
      container.addEndpoint(echoConfig);
    });

    ContextHandlerCollection handlers = new ContextHandlerCollection();
    handlers.addHandler(handler);

    server.setHandler(handlers);

    server.start();
    server.join();
  }
}
