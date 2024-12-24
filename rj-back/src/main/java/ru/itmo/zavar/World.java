package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;
import reactor.core.publisher.Mono;

@Getter
public class World {
    public static final Logger logger = Logger.getLogger(World.class.getName());

    public PostOffices offices = new PostOffices();
    public Transports transports = new Transports();
    public Actors actors = new Actors();

    public class Actors {
        private Map<Actor, Actor.Schedule> actors = new HashMap<>();

        public void add(Actor actor) {
            if (actors.containsKey(actor)) {
                logger.log(Level.INFO, "actor already exists");
                return;
            }
            actors.put(actor, actor.new Schedule());
            logger.log(Level.INFO, "added actor " + actor.toString());
        }

        public Actor.Schedule getSchedule(Actor actor) {
            Actor.Schedule schedule = actors.get(actor);
            if (schedule == null) {
                logger.log(Level.WARNING, "actor doesn't exist");
                return null;
            }
            return schedule;
        }

        public void receiveMail(Actor actor, PostOffice postOffice) {
            Actor.Schedule schedule = this.getSchedule(actor);
            // schedule.addTaskMailReceive();
        }

        public void sendMail(Actor actor, PostOffice office, Mail mail) {
            Actor.Schedule schedule = this.getSchedule(actor);
            PostOffice postOffice = offices.get(office);
            Mono<Mail> mono = schedule.addTaskMailSend(mail);
            postOffice.applyMailOne(mono);
        }
    }

    public class PostOffices {
        private Map<PostOffice, Void> postOffices = new HashMap<>();

        public void add(PostOffice office) {
            if (postOffices.containsKey(office)) {
                logger.log(Level.INFO, "office already exists");
                return;
            }
            postOffices.put(office, null);
            logger.log(Level.INFO, "added office " + office.toString());
        }

        public PostOffice get(PostOffice office) {
            if (postOffices.containsKey(office)) {
                return office;
            }
            logger.log(Level.INFO, "post office isn't present");
            return null;
        }
    }

    public class Transports {
        private Map<Transport, Void> transports = new HashMap<>();

        public void add(Transport transport) {
            if (transports.containsKey(transport)) {
                logger.log(Level.INFO, "transport already exists");
                return;
            }
            transports.put(transport, null);
            logger.log(Level.INFO, "added transport " + transport.toString());
        }
    }
}
