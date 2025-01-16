package ru.itmo.zavar.websocket.pipeline;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import reactor.core.publisher.Flux;
import ru.itmo.zavar.model.*;
import ru.itmo.zavar.model.actor.Actor;
import ru.itmo.zavar.websocket.Session;
import ru.itmo.zavar.websocket.SessionManager;
import ru.itmo.zavar.websocket.event.*;
import ru.itmo.zavar.websocket.message.*;
import ru.itmo.zavar.websocket.pack.*;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

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
                        PackMappers.coordinatesFromPack(args.getCoordinates()));
                world.postoffices.add(postOffice);
            }
            case AddTransportMessage.OP -> {
                AddTransportMessage addTransportMsg = (AddTransportMessage) msg;
                AddTransportMessage.Args args = addTransportMsg.getArgs();

                try {
                    Transport transport = new Transport(
                            args.getCapacity(),
                            args.getSpeed(),
                            args.getOps(),
                            args.getLicensePlate(),
                            PackMappers.coordinatesFromPack(args.getCoordinates()));
                    world.transports.add(transport);
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
                switch (msg) {
                    case AddTaskMessage.SendMailOneMessage sendMailOneMessage -> {
                        TaskPack.SendMailOne task = sendMailOneMessage.getArgs().getTask();
                        Mail mail = new Mail(
                                world.mailIds.getAndIncrement(),
                                task.getMail().getSubject(),
                                world.actors.getActor(task.getMail().getSenderActorId()),
                                world.actors.getActor(task.getMail().getReceiverActorId()),
                                new Mail.Data(task.getMail().getText()),
                                0);
                        world.actors.sendMail(
                                task.getActorId(),
                                task.getPostofficeId(),
                                mail);
                    }
                    case AddTaskMessage.SendMailRepeatedMessage sendMailRepeatedMessage -> {
                        TaskPack.SendMailRepeated task = sendMailRepeatedMessage.getArgs().getTask();
                        world.actors.sendMailFlux(task.getActorId(),
                                task.getPostofficeId(),
                                Flux.generate(AtomicLong::new, (state, sink) -> {
                                    Mail mail = new Mail(
                                            world.mailIds.getAndIncrement(),
                                            task.getMail().getSubject(),
                                            world.actors.getActor(task.getMail().getSenderActorId()),
                                            world.actors.getActor(task.getMail().getReceiverActorId()),
                                            new Mail.Data(task.getMail().getText()),
                                            0);
                                    sink.next(mail);
                                    return state;
                                }).delayElements(Duration.ofMillis(task.getIntervalMs())).cast(Mail.class));
                    }
                    case AddTaskMessage.ReceiveMailOneMessage receiveMailOneMessage -> {
                        TaskPack.ReceiveMailOne task = receiveMailOneMessage.getArgs().getTask();
                        world.actors.receiveMail(
                                task.getActorId(),
                                task.getPostofficeId(),
                                (mail, actor) -> {
                                    logger.info(mail.toString());
                                    actor.addMailToBuffer(mail);
                                },
                                e -> {
                                    logger.warning(e.toString());
                                    ctx.writeAndFlush(new WarningResponse("Error receiving mail " + e));
                                });
                    }
                    case AddTaskMessage.ReceiveMailManyMessage receiveMailManyMessage -> {
                        TaskPack.ReceiveMailMany task = receiveMailManyMessage.getArgs().getTask();
                        world.actors.receiveMailMany(
                                task.getActorId(),
                                task.getPostofficeId(),
                                (mail, actor) -> {
                                    logger.info(mail.toString());
                                    actor.addMailToBuffer(mail);
                                },
                                e -> {
                                    logger.warning(e.toString());
                                    ctx.writeAndFlush(new WarningResponse("Error receiving mail " + e));
                                });
                    }
                    default -> ctx.writeAndFlush(new ErrorResponse(msg, "unsupported kind task"));
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
            case TransportSetRouteMessage.OP -> {
                TransportSetRouteMessage setRouteMsg = (TransportSetRouteMessage) msg;
                TransportSetRouteMessage.Args args = setRouteMsg.getArgs();
                Transport transport = world.transports.get(args.getTransportId());
                if (transport == null) {
                    ctx.writeAndFlush(new ErrorResponse(msg, "transport not found"));
                } else {
                    List<Postoffice> postoffices = args.getRoute().getPostOfficeIds().stream()
                            .map(aLong -> world.postoffices.get(aLong)).toList();
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
            case StopTaskMessage.OP -> {
                StopTaskMessage stopTaskMsg = (StopTaskMessage) msg;
                StopTaskMessage.Args args = stopTaskMsg.getArgs();
                world.actors.deleteActorTask(args.getActorId(), args.getTaskId());
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
