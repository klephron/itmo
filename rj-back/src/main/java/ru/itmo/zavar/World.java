package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import reactor.core.publisher.Mono;

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
            logger.fine("closed " + office.toString());
        }
    }

    public class Actors {
        private Map<Actor, Actor.Schedule> entries = new HashMap<>();

        public void add(Actor actor) {
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

        public Actor get(Actor actor) {
            if (entries.containsKey(actor)) {
                return actor;
            }
            logger.log(Level.INFO, "actot doesn't exists");
            return null;
        }

        public void receiveMail(Actor _actor, PostOffice _office, Consumer<Mail> onOne, Consumer<Throwable> onError) {
            Actor actor = actors.get(_actor);
            PostOffice office = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            Mono<Mail> mono = office.receiveMailOne(actor);
            schedule.addTaskMailReceive(mono, onOne, onError);
        }

        public void receiveMail(Actor _actor, PostOffice _office, Consumer<Mail> onOne) {
            receiveMail(_actor, _office, onOne, (e) -> {
                logger.warning(e.toString());
            });
        }

        public void sendMail(Actor _actor, PostOffice _office, Mail mail) {
            Actor actor = actors.get(_actor);
            PostOffice postOffice = offices.get(_office);
            Actor.Schedule schedule = this.getSchedule(actor);
            Mono<Mail> mono = schedule.addTaskMailSend(mail);
            postOffice.applyMailOne(mono);
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
                logger.fine("opened office " + _office.toString());
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
            logger.fine("added transport " + transport.toString());
        }
    }
}
