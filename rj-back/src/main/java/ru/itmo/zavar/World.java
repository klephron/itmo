package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;

@Getter
public class World {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public PostOffices offices = new PostOffices();
    public Transports transports = new Transports();
    public Actors actors = new Actors();

    public class Actors {
        private Map<Actor, Void> actors = new HashMap<>();

        public void add(Actor actor) {
            if (actors.containsKey(actor)) {
                logger.log(Level.INFO, "actor already exists");
                return;
            }
            actors.put(actor, null);
            logger.log(Level.INFO, "added actor " + actor.toString());
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
