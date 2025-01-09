package ru.itmo.zavar.websocket.handler;

import static ru.itmo.zavar.World.logger;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import ru.itmo.zavar.websocket.SessionManager;

@ChannelHandler.Sharable
public class WebSocketInitialHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    private SessionManager sessionManager;

    public WebSocketInitialHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (frame instanceof TextWebSocketFrame) {
            String request = ((TextWebSocketFrame) frame).text().trim();
            logger.fine("received '" + request + "'");
            String response = request.toUpperCase();
            ctx.channel().writeAndFlush(new TextWebSocketFrame(response));
            logger.fine("sent '" + response + "'");
        } else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionManager.removeSession(ctx.channel());
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        this.sessionManager.addSession(ctx.channel());
    }
}
