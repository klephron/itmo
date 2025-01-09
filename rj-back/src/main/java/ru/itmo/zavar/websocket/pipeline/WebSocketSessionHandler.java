package ru.itmo.zavar.websocket.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import ru.itmo.zavar.websocket.Session;
import ru.itmo.zavar.websocket.SessionManager;
import ru.itmo.zavar.websocket.event.SessionEvent;

// NOTE: can be optimized by replacing this handler with another, but will make state code duplicated
@ChannelHandler.Sharable
public class WebSocketSessionHandler extends SimpleChannelInboundHandler<WebSocketFrame> {
    public static final String NAME = "session";

    private final SessionManager sessionManager;
    private final WebSocketWorldHandler webSocketWorldHandler;

    public WebSocketSessionHandler(SessionManager sessionManager, WebSocketWorldHandler webSocketWorldHandler) {
        this.sessionManager = sessionManager;
        this.webSocketWorldHandler = webSocketWorldHandler;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception {
        ctx.fireChannelRead(msg.retain());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        sessionManager.removeSession(ctx.channel());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            Session session = this.sessionManager.addSession(ctx.channel());
            ctx.channel().writeAndFlush(new SessionEvent(session, "created"));
            ctx.pipeline().addAfter(NAME, WebSocketWorldHandler.NAME, webSocketWorldHandler);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
