package ru.itmo.zavar.websocket.pipeline;

import static ru.itmo.zavar.World.logger;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import ru.itmo.zavar.websocket.event.ErrorResponse;
import ru.itmo.zavar.websocket.message.*;

@ChannelHandler.Sharable
public class WebSocketTextDecoder extends MessageToMessageDecoder<TextWebSocketFrame> {
    private final Gson gson = new Gson();

    @Override
    protected void decode(ChannelHandlerContext ctx, TextWebSocketFrame frame, List<Object> out) throws Exception {
        final String json = frame.text();
        try {
            Message message = gson.fromJson(json, Message.class);
            if (message == null) {
                ctx.writeAndFlush(new ErrorResponse(message, "message is null"));
                return;
            }

            switch (message.getOp()) {
                case SubscribeMessage.OP -> out.add(gson.fromJson(json, SubscribeMessage.class));
                case AddPostofficeMessage.OP -> out.add(gson.fromJson(json, AddPostofficeMessage.class));
                case AddTransportMessage.OP -> out.add(gson.fromJson(json, AddTransportMessage.class));
                case AddActorIndividualMessage.OP -> out.add(gson.fromJson(json, AddActorIndividualMessage.class));
                case AddActorOrganizationMessage.OP -> out.add(gson.fromJson(json, AddActorOrganizationMessage.class));
                case AddTaskMessage.OP -> out.add(gson.fromJson(json, AddTaskMessage.class));
                case AddActorMessage.OP -> {
                    AddActorMessage addActorMessage = gson.fromJson(json, AddActorMessage.class);
                    switch (addActorMessage.getArgs().getActor().getKind()) {
                        case INDIVIDUAL -> {
                            out.add(gson.fromJson(json, AddActorIndividualMessage.class));
                        }
                        case ORGANIZATION -> {
                            out.add(gson.fromJson(json, AddActorOrganizationMessage.class));
                        }
                    }
                }
                default -> out.add(message);
            }
            // otherwise add just a frame
        } catch (JsonSyntaxException e) {
            logger.warning(e.getMessage());
            ctx.writeAndFlush(new ErrorResponse(json, "bad json syntax"));
        } catch (Exception e) {
            logger.warning(e.getMessage());
            e.printStackTrace();
        }

    }

}
