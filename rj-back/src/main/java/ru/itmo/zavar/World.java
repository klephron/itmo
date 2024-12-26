package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.itmo.zavar.Actor.Schedule.Task;

@Getter
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

    public PostOffices offices = new PostOffices();
    public Transports transports = new Transports();
    public Actors actors = new Actors();

    public void stop() {
        for (var office : offices.entries.keySet()) {
            office.close();
            logger.fine("closed " + office.getAddress());
        }
    }

    public class Actors {
        private Map<Actor, Actor.Schedule> entries = new HashMap<>();

        public void addActor(Actor actor) {
            if (entries.containsKey(actor)) {
                logger.log(Level.INFO, "actor already exists");
                return;
            }
            entries.put(actor, actor.new Schedule());
            logger.fine("added actor " + actor.toString());
        }

        public Actor.Schedule getSchedule(Actor actor) {
            Actor.Schedule schedule = entries.get(actor);
            if (schedule == null) {
                logger.warning("actor doesn't exist");
                return null;
            }
            return schedule;
        }

        public Actor getActor(Actor actor) {
            if (entries.containsKey(actor)) {
                return actor;
            }
            logger.info("actor doesn't exists");
            return null;
        }

        public Task receiveMail(Actor _actor, PostOffice _office, Consumer<Mail> onOne, Consumer<Throwable> onError) {
            Actor actor = actors.getActor(_actor);
            PostOffice office = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            Mono<Mail> mono = office.receiveMailOne(actor);
            var pair = schedule.addTaskMailReceiveOne(mono, onOne, onError);
            return pair.left;
        }

        public Task receiveMail(Actor _actor, PostOffice _office, Consumer<Mail> onOne) {
            return receiveMail(_actor, _office, onOne, (e) -> {
                logger.warning(e.toString());
            });
        }

        public Task receiveMailMany(Actor _actor, PostOffice _office, Consumer<Mail> onOne,
                Consumer<Throwable> onError) {
            Actor actor = actors.getActor(_actor);
            PostOffice office = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            Flux<Mail> flux = office.receiveMailFlux(actor);
            var pair = schedule.addTaskMailReceiveFlux(flux, onOne, onError);
            return pair.left;
        }

        public Task receiveMailMany(Actor _actor, PostOffice _office, Consumer<Mail> onOne) {
            return receiveMailMany(_actor, _office, onOne, (e) -> {
                logger.warning(e.toString());
            });
        }

        public Task sendMail(Actor _actor, PostOffice _office, Mail mail) {
            Actor actor = actors.getActor(_actor);
            PostOffice postOffice = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            var pair = schedule.addTaskMailSendOne(mail);
            postOffice.applyMailMono(pair.right);
            return pair.left;
        }

        public Task sendMailFlux(Actor _actor, PostOffice _office, Flux<Mail> _flux) {
            Actor actor = actors.getActor(_actor);
            PostOffice postOffice = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            var pair = schedule.addTaskMailSendFlux(_flux);
            postOffice.applyMailFlux(pair.right);
            return pair.left;
        }

        public void deleteActorTask(Actor _actor, Task task) {
            Actor actor = actors.getActor(_actor);
            Actor.Schedule schedule = this.getSchedule(actor);
            schedule.deleteTask(task);
        }
    }

    public class PostOffices {
        private Map<PostOffice, Void> entries = new HashMap<>();

        public void add(PostOffice _office) {
            if (entries.containsKey(_office)) {
                logger.info("office already exists");
                return;
            }
            entries.put(_office, null);
            PostOffice office = this.get(_office);
            if (office != null) {
                office.open();
                logger.fine("opened office " + office.getAddress());
            }
        }

        public PostOffice get(PostOffice office) {
            if (entries.containsKey(office)) {
                return office;
            }
            logger.info("post office isn't present");
            return null;
        }
    }

    public class Transports {
        private Map<Transport, Void> entries = new HashMap<>();

        public void add(Transport transport) {
            if (entries.containsKey(transport)) {
                logger.log(Level.INFO, "transport already exists");
                return;
            }
            entries.put(transport, null);
            logger.fine("added transport " + transport.getNumber());
        }

        public Transport get(Transport transport) {
            if (entries.containsKey(transport)) {
                return transport;
            }
            logger.info("transport isn't present");
            return null;
        }

        public void setRoute(Transport _transport, Route<PostOffice> route) {
            Transport transport = this.get(_transport);
            transport.setRoute(route);
        }

        public void start(Transport _transport, int index) {
            Transport transport = this.get(_transport);
            transport.start(index);
        }

        public void start(Transport _transport) {
            Transport transport = this.get(_transport);
            transport.start();
        }

        public void stop(Transport _transport) {
            Transport transport = this.get(_transport);
            transport.stop();
        }
    }
}
