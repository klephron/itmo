package ru.itmo.zavar.websocket.pipeline;

import static ru.itmo.zavar.World.logger;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ru.itmo.zavar.websocket.event.ErrorEvent;
import ru.itmo.zavar.websocket.message.Message;
import ru.itmo.zavar.websocket.message.SubscribeMessage;

@ChannelHandler.Sharable
public class WebSocketTextDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {
    private final Gson gson = new Gson();

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) throws Exception {
        final String json = frame.text();
        try {
            Message message = gson.fromJson(json, Message.class);
            if (message == null) {
                ctx.writeAndFlush(new ErrorEvent(message, "message is null"));
                return;
            }

            switch (message.getOp()) {
                case SubscribeMessage.OP:
                    out.add(gson.fromJson(json, SubscribeMessage.class));
                    break;
                default:
                    out.add(message);
                    break;
            }
            // otherwise add just a frame
        } catch (JsonSyntaxException e) {
            logger.warning(e.getMessage());
            ctx.writeAndFlush(new ErrorEvent(json, "bad json syntax"));
        }

    }

}
