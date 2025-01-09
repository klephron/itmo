package ru.itmo.zavar.websocket.pipeline;

import java.util.List;

import com.google.gson.Gson;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ru.itmo.zavar.websocket.event.Event;

@ChannelHandler.Sharable
public class WebSocketTextEncoder extends MessageToMessageEncoder<Event> {
    private final Gson gson = new Gson();

    @Override
    protected void encode(ChannelHandlerContext arg0, Event msg, List<Object> out) throws Exception {
        out.add(new TextWebSocketFrame(gson.toJson(msg)));
    }

}
