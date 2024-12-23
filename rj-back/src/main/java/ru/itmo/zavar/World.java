package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import lombok.Getter;

@Getter
public class World {
    private static final Logger logger = Logger.getLogger(World.class.getName());

    public PostOffices offices = new PostOffices();
    public Transports transports = new Transports();

    public class PostOffices {
        Elements<PostOffice> postOffices = new Elements<>();

        public Long add(PostOffice office) {
            var r = postOffices.insert(office);
            logger.log(Level.INFO, "created postOffice with id=" + r.toString());
            return r;
        }
    }

    public class Transports {
        Elements<Transport> transports = new Elements<>();

        public Long add(Transport transport) {
            var r = transports.insert(transport);
            logger.log(Level.INFO, "created transport with id=" + r.toString());
            return r;
        }
    }

    private class Elements<T> {
        private AtomicLong counter = new AtomicLong(0L);
        private final Map<Long, T> elements = new HashMap<>();

        public Long insert(T e) {
            Long id = counter.incrementAndGet();
            elements.put(id, e);
            return id;
        }

        public Long remove(Long id) {
            T e = elements.remove(id);
            if (e == null) {
                return 0L;
            }
            return id;
        }
    }
}
