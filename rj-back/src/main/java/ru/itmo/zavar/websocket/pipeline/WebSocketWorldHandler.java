package ru.itmo.zavar.websocket.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.itmo.zavar.websocket.event.ErrorEvent;
import ru.itmo.zavar.websocket.event.Event;
import ru.itmo.zavar.websocket.message.Message;
import ru.itmo.zavar.websocket.message.SubscribeMessage;

@ChannelHandler.Sharable
public class WebSocketWorldHandler extends SimpleChannelInboundHandler<Message> {
    public static final String NAME = "world";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.getOp()) {
            case SubscribeMessage.OP:
                SubscribeMessage subscribeMessage = (SubscribeMessage) msg;
                ctx.writeAndFlush(new Event(subscribeMessage.getOp()));
                return;
            default:
                ctx.writeAndFlush(new ErrorEvent(msg, "unknown op"));
                break;
        }
    }

}
