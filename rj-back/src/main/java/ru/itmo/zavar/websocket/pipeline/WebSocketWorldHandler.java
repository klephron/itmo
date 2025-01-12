package ru.itmo.zavar.websocket.pipeline;

import static ru.itmo.zavar.World.logger;

import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.itmo.zavar.World;
import ru.itmo.zavar.websocket.Session;
import ru.itmo.zavar.websocket.SessionManager;
import ru.itmo.zavar.websocket.event.ErrorResponse;
import ru.itmo.zavar.websocket.event.Event;
import ru.itmo.zavar.websocket.event.SubscribeEvent;
import ru.itmo.zavar.websocket.event.SubscribeResponse;
import ru.itmo.zavar.websocket.message.Message;
import ru.itmo.zavar.websocket.message.SubscribeMessage;
import ru.itmo.zavar.websocket.pack.ActorPack;
import ru.itmo.zavar.websocket.pack.PackMappers;
import ru.itmo.zavar.websocket.pack.PostOfficePack;
import ru.itmo.zavar.websocket.pack.TransportPack;

@ChannelHandler.Sharable
public class WebSocketWorldHandler extends SimpleChannelInboundHandler<Message> {
    public static final String NAME = "world";
    private final SessionManager sessionManager;
    private final World world;

    public WebSocketWorldHandler(SessionManager sessionManager, World world) {
        this.sessionManager = sessionManager;
        this.world = world;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.getOp()) {
            case SubscribeMessage.OP:
                SubscribeMessage subscribeMsg = (SubscribeMessage) msg;
                ctx.writeAndFlush(subscribe(ctx, subscribeMsg));
                return;
            default:
                ctx.writeAndFlush(new ErrorResponse(msg, "unknown op"));
                break;
        }
    }

    private Event subscribe(ChannelHandlerContext ctx, SubscribeMessage msg) {
        Session session = sessionManager.getSession(ctx.channel());
        if (session == null) {
            return new ErrorResponse(msg, "no session exists");
        }
        if (msg.getArgs() == null) {
            return new ErrorResponse(msg, "no args specified");
        }

        var status = SubscribeResponse.Status.CREATED;

        if (session.getSubscription() != null) {
            status = SubscribeResponse.Status.RECREATED;
            session.getSubscription().cancel();
        }

        session.setSubscription(session.new WorldSubscription(
                msg.getArgs().getUps(),
                this::subscribeOnUpdateNext,
                msg.getArgs().getPostOfficeIds(),
                msg.getArgs().getTransportIds(),
                msg.getArgs().getActorIds()));

        session.getSubscription().start();

        return new SubscribeResponse(
                new SubscribeResponse.Args(
                        session.getSubscription().getUps(),
                        session.getSubscription().getPostOfficeIds().stream().toList(),
                        session.getSubscription().getTransportIds().stream().toList(),
                        session.getSubscription().getActorIds().stream().toList()),
                status);
    }

    private boolean subscribeOnUpdateNext(Session.Subscription subscription, Channel channel) {
        Session.WorldSubscription worldSub = (Session.WorldSubscription) subscription;

        List<PostOfficePack> postOfficePacks = world.offices.getAll().stream()
                .map(p -> worldSub.getPostOfficeIds().contains(p.getId()) ? PackMappers.postOfficeToPack(p)
                        : PackMappers.PostOfficeToPackEssential(p))
                .toList();

        List<TransportPack> transportPacks = world.transports.getAll().stream()
                .map(p -> worldSub.getTransportIds().contains(p.getId()) ? PackMappers.transportToPack(p)
                        : PackMappers.transportToPackEssential(p))
                .toList();

        List<ActorPack> actorPacks = world.actors.getActorAll().stream()
                .map(p -> worldSub.getActorIds().contains(p.getId()) ? PackMappers.actorToPack(p)
                        : PackMappers.actorToPackEssential(p))
                .toList();

        channel.writeAndFlush(
                new SubscribeEvent(
                        new SubscribeEvent.Args(channel.id().asLongText(), postOfficePacks, transportPacks,
                                actorPacks)));
        return true;
    }

}
