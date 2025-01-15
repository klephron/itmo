package ru.itmo.zavar.websocket.pipeline;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import reactor.core.publisher.Flux;
import ru.itmo.zavar.*;
import ru.itmo.zavar.websocket.Session;
import ru.itmo.zavar.websocket.SessionManager;
import ru.itmo.zavar.websocket.event.*;
import ru.itmo.zavar.websocket.message.*;
import ru.itmo.zavar.websocket.pack.*;

@ChannelHandler.Sharable
public class WebSocketWorldHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger logger = Logger.getLogger(WebSocketWorldHandler.class.getName());
    public static final String NAME = "world";
    private final SessionManager sessionManager;
    private final World world;

    public WebSocketWorldHandler(SessionManager sessionManager, World world) {
        this.sessionManager = sessionManager;
        this.world = world;
    }

    //TODO
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        switch (msg.getOp()) {
            case SubscribeMessage.OP -> {
                SubscribeMessage subscribeMsg = (SubscribeMessage) msg;
                ctx.writeAndFlush(subscribe(ctx, subscribeMsg));
            }
            case AddPostofficeMessage.OP -> {
                AddPostofficeMessage addPostofficeMsg = (AddPostofficeMessage) msg;
                AddPostofficeMessage.Args args = addPostofficeMsg.getArgs();
                Postoffice postOffice = new Postoffice(
                    PackMappers.addressFromPack(args.getAddress()),
                    PackMappers.coordinatesFromPack(args.getCoordinates())
                );
                world.postoffices.add(postOffice);
            }
            case AddTransportMessage.OP -> {
                AddTransportMessage addTransportMsg = (AddTransportMessage) msg;
                AddTransportMessage.Args args = addTransportMsg.getArgs();

                //List<Postoffice> postOffices = args.getRoute().getPostOfficeIds().stream().map(aLong -> world.postoffices.get(aLong)).toList();
                //Route<Postoffice> postofficeRoute = new Route<>(postOffices, args.getRoute().getIsCyclic());

                try {
                    Transport transport = new Transport(
                        args.getCapacity(),
                        args.getSpeed(),
                        args.getOps(),
                        args.getLicensePlate(),
                        PackMappers.coordinatesFromPack(args.getCoordinates())
                    );
                    world.transports.add(transport);
                    //world.transports.setRoute(transport.getId(), postofficeRoute);
                    //TODO
                    //world.transports.start(transport.getId(), 0);
                } catch (NoSuchElementException e) {
                    ctx.writeAndFlush(new ErrorResponse(msg, e.getMessage()));
                }
            }
            case AddActorIndividualMessage.OP -> {
                AddActorIndividualMessage addActorMsg = (AddActorIndividualMessage) msg;
                AddActorIndividualMessage.Args args = addActorMsg.getArgs();
                Actor actor = PackMappers.actorFromPack(args.getActor());
                world.actors.addActor(actor);
            }
            case AddActorOrganizationMessage.OP -> {
                AddActorOrganizationMessage addActorMsg = (AddActorOrganizationMessage) msg;
                AddActorOrganizationMessage.Args args = addActorMsg.getArgs();
                Actor actor = PackMappers.actorFromPack(args.getActor());
                world.actors.addActor(actor);
            }
            case AddTaskMessage.OP -> {
                AddTaskMessage addTaskMsg = (AddTaskMessage) msg;
                AddTaskMessage.Args args = addTaskMsg.getArgs();
                switch (args.getKind()) {
                    case SEND_MAIL_ONE -> {
                        Mail mail = new Mail(
                            world.mailIds.getAndIncrement(),
                            args.getMailSimplePack().getSubject(),
                            world.actors.getActor(args.getMailSimplePack().getSenderId()),
                            world.actors.getActor(args.getMailSimplePack().getReceiverId()),
                            new Mail.Data(args.getMailSimplePack().getText()),
                            0
                        );
                        world.actors.sendMail(
                            args.getActorId(),
                            args.getPostofficeId(),
                            mail
                        );
                    }
                    case RECEIVE_MAIL_ONE -> {
                        world.actors.receiveMail(
                            args.getActorId(),
                            args.getPostofficeId(),
                            m -> {
                                logger.info(m.toString());
                            },
                            e -> {
                                logger.warning(e.toString());
                                ctx.writeAndFlush(new WarningResponse("Error receiving mail " + e));
                            }
                        );
                    }
                    case SEND_MAIL_REPEATED -> {
                        Mail mail = new Mail(
                            world.mailIds.getAndIncrement(),
                            args.getMailSimplePack().getSubject(),
                            world.actors.getActor(args.getMailSimplePack().getSenderId()),
                            world.actors.getActor(args.getMailSimplePack().getReceiverId()),
                            new Mail.Data(args.getMailSimplePack().getText()),
                            0
                        );
                        Actor.Schedule.Task sendMailFlux = world.actors.sendMailFlux(args.getActorId(), args.getPostofficeId(),
                            Flux.generate(AtomicLong::new, (state, sink) -> {
                                sink.next(mail);
                                return state;
                            }).delayElements(Duration.ofMillis(args.getInterval())).cast(Mail.class));
                    }
                    case RECEIVE_MAIL_MANY -> {
                        world.actors.receiveMailMany(
                            args.getActorId(),
                            args.getPostofficeId(),
                            (mail, actor) -> {
                                logger.info(mail.toString());

                            },
                            e -> {
                                logger.warning(e.toString());
                                ctx.writeAndFlush(new WarningResponse("Error receiving mail " + e));
                            }
                        );
                    }
                    default -> ctx.writeAndFlush(new ErrorResponse(msg, "unsupported kind " + args.getKind()));
                }
            }
            case AddActorMessage.OP -> {
                if (msg instanceof AddActorIndividualMessage addActorMsg) {
                    AddActorIndividualMessage.Args args = addActorMsg.getArgs();
                    Actor actor = PackMappers.actorFromPack(args.getActor());
                    world.actors.addActor(actor);
                } else if (msg instanceof AddActorOrganizationMessage addActorMsg) {
                    AddActorOrganizationMessage.Args args = addActorMsg.getArgs();
                    Actor actor = PackMappers.actorFromPack(args.getActor());
                    world.actors.addActor(actor);
                }
            }
            case SetRouteMessage.OP -> {
                SetRouteMessage setRouteMsg = (SetRouteMessage) msg;
                SetRouteMessage.Args args = setRouteMsg.getArgs();
                Transport transport = world.transports.get(args.getTransportId());
                if (transport == null) {
                    ctx.writeAndFlush(new ErrorResponse(msg, "transport not found"));
                } else {
                    List<Postoffice> postoffices = args.getRoute().getPostOfficeIds().stream().map(aLong -> world.postoffices.get(aLong)).toList();
                    Route<Postoffice> postofficeRoute = new Route<>(postoffices, args.getRoute().getIsCyclic());
                    world.transports.setRoute(args.getTransportId(), postofficeRoute);
                }
            }
            case TransportStartMessage.OP -> {
                TransportStartMessage transportStartMsg = (TransportStartMessage) msg;
                TransportStartMessage.Args args = transportStartMsg.getArgs();
                Transport transport = world.transports.get(args.getTransportId());
                if (transport == null) {
                    ctx.writeAndFlush(new ErrorResponse(msg, "transport not found"));
                }
                world.transports.start(args.getTransportId(), args.getIndex());
            }
            case TransportStopMessage.OP -> {
                TransportStopMessage transportStopMsg = (TransportStopMessage) msg;
                TransportStopMessage.Args args = transportStopMsg.getArgs();
                Transport transport = world.transports.get(args.getTransportId());
                if (transport == null) {
                    ctx.writeAndFlush(new ErrorResponse(msg, "transport not found"));
                }
                world.transports.stop(args.getTransportId());
            }
            default -> ctx.writeAndFlush(new ErrorResponse(msg, "unknown op"));
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
                msg.getArgs().getPostofficeIds(),
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

        List<PostofficePack> postOfficePacks = world.postoffices.getAll().stream()
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
