package ru.itmo.zavar.model;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.zavar.log.LogFormatter;
import ru.itmo.zavar.model.actor.Actor;
import ru.itmo.zavar.model.actor.Actor.Schedule.Task;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class World {
    public static final Logger logger = Logger.getLogger(World.class.getName());
    static {
        logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
        logger.setLevel(Level.ALL);
    }

    public Postoffices postoffices = new Postoffices();
    public Transports transports = new Transports();
    public Actors actors = new Actors();
    public AtomicLong mailIds = new AtomicLong();

    public void stop() {
        for (var office : postoffices.entries.values()) {
            office.close();
            logger.fine("closed " + office.getAddress());
        }
    }

    public class Actors {
        private final Map<Long, Actor> entries = new HashMap<>();
        private long idCounter = 0;

        public void addActor(Actor actor) {
            if (actor.getId() != null && entries.containsKey(actor.getId())) {
                logger.log(Level.INFO, "actor already exists");
                return;
            }
            actor.setId(++idCounter);
            entries.put(actor.getId(), actor);
            logger.fine("added actor " + actor.toString());
        }

        public Actor.Schedule getSchedule(long actorId) {
            Actor.Schedule schedule = entries.get(actorId).getSchedule();
            if (schedule == null) {
                logger.warning("actor doesn't exist");
                return null;
            }
            return schedule;
        }

        public Actor getActor(long actorId) {
            if (!entries.containsKey(actorId)) {
                logger.info("actor doesn't exists");
                return null;
            }
            return entries.get(actorId);
        }

        public Collection<Actor> getActorAll() {
            return Collections.unmodifiableCollection(entries.values());
        }

        public Task receiveMail(long actorId, long postofficeId, BiConsumer<Mail, Actor> onOne, Consumer<Throwable> onError) {
            Actor actor = actors.getActor(actorId);
            Postoffice office = postoffices.get(postofficeId);
            Actor.Schedule schedule = actor.getSchedule();
            Mono<Mail> mono = office.receiveMailOne(actor);
            var pair = schedule.addTaskMailReceiveOne(mono, onOne, onError);
            return pair.left;
        }

        public Task receiveMail(long actorId, long postofficeId, BiConsumer<Mail, Actor> onOne) {
            return receiveMail(actorId, postofficeId, onOne, (e) -> {
                logger.warning(e.toString());
            });
        }

        public Task receiveMailMany(long actorId, long postofficeId, BiConsumer<Mail, Actor> onOne,
                                    Consumer<Throwable> onError) {
            Actor actor = actors.getActor(actorId);
            Postoffice office = postoffices.get(postofficeId);
            Actor.Schedule schedule = actor.getSchedule();
            Flux<Mail> flux = office.receiveMailFlux(actor);
            var pair = schedule.addTaskMailReceiveFlux(flux, onOne, onError);
            return pair.left;
        }

        public Task receiveMailMany(long actorId, long postofficeId, BiConsumer<Mail, Actor> onOne) {
            return receiveMailMany(actorId, postofficeId, onOne, (e) -> {
                logger.warning(e.toString());
            });
        }

        public Task sendMail(long actorId, long postofficeId, Mail mail) {
            Actor actor = actors.getActor(actorId);
            Postoffice postOffice = postoffices.get(postofficeId);
            Actor.Schedule schedule = actor.getSchedule();
            var pair = schedule.addTaskMailSendOne(mail);
            postOffice.applyMailMono(pair.right);
            return pair.left;
        }

        public Task sendMailFlux(long actorId, long postofficeId, Flux<Mail> _flux) {
            Actor actor = actors.getActor(actorId);
            Postoffice postOffice = postoffices.get(postofficeId);
            Actor.Schedule schedule = actor.getSchedule();
            var pair = schedule.addTaskMailSendFlux(_flux);
            postOffice.applyMailFlux(pair.right);
            return pair.left;
        }

        public void deleteActorTask(long actorId, long taskId) {
            Actor actor = actors.getActor(actorId);
            Actor.Schedule schedule = actor.getSchedule();
            schedule.deleteTask(taskId);
        }
    }

    public class Postoffices {
        private final Map<Long, Postoffice> entries = new HashMap<>();
        private long idCounter = 0;

        public void add(Postoffice office) {
            if (office.getId() != null && entries.containsKey(office.getId())) {
                logger.info("office already exists");
                return;
            }
            office.setId(++idCounter);
            entries.put(office.getId(), office);
            office.open();
            logger.fine("opened office " + office.getAddress());
        }

        public Postoffice get(long postOfficeId) {
            if (!entries.containsKey(postOfficeId)) {
                logger.info("post office isn't present");
                throw new NoSuchElementException("post office " + postOfficeId + " is not present");
            }
            return entries.get(postOfficeId);
        }

        public Collection<Postoffice> getAll() {
            return Collections.unmodifiableCollection(entries.values());
        }
    }

    public class Transports {
        private final Map<Long, Transport> entries = new HashMap<>();
        private long idCounter = 0;

        public void add(Transport transport) {
            if (transport.getId() != null && entries.containsKey(transport.getId())) {
                logger.log(Level.INFO, "transport already exists");
                return;
            }
            transport.setId(++idCounter);
            entries.put(transport.getId(), transport);
            logger.fine("added transport " + transport.getLicensePlate());
        }

        public Transport get(long transportId) {
            if (!entries.containsKey(transportId)) {
                logger.info("transport isn't present");
                return null;
            }
            return entries.get(transportId);
        }

        public Collection<Transport> getAll() {
            return Collections.unmodifiableCollection(entries.values());
        }

        public void setRoute(long transportId, Route<Postoffice> route) {
            Transport transport = this.get(transportId);
            transport.setRoute(route);
        }

        public void start(long transportId, int index) {
            Transport transport = this.get(transportId);
            transport.start(index);
        }

        public void start(long transportId) {
            Transport transport = this.get(transportId);
            transport.start();
        }

        public void stop(long transportId) {
            Transport transport = this.get(transportId);
            transport.stop();
        }
    }
}
