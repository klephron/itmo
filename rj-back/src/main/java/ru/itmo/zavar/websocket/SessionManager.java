package ru.itmo.zavar.websocket;

import static ru.itmo.zavar.World.logger;

import java.util.HashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionManager {
    private final HashMap<ChannelId, Session> sessions = new HashMap<>();

    public Session addSession(Channel channel) {
        var session = new Session(channel);
        return this.addSession(session);
    }

    public Session addSession(Session session) {
        sessions.put(session.getChannelId(), session);
        logger.info("added " + session);
        return session;
    }

    public Session removeSession(Session session) {
        return this.removeSession(session.getChannel());
    }

    public Session removeSession(Channel channel) {
        Session session = sessions.remove(channel.id());
        logger.info("removed " + session);
        return session;
    }

    public Session getSession(Channel channel) {
        return sessions.get(channel.id());
    }
}
