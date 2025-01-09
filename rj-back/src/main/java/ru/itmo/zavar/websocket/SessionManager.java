package ru.itmo.zavar.websocket;

import static ru.itmo.zavar.World.logger;

import java.util.HashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SessionManager {
    @Getter
    private final HashMap<ChannelId, Session> sessions = new HashMap<>();

    public void addSession(Channel channel) {
        this.addSession(new Session(channel));
    }

    public void addSession(Session session) {
        sessions.put(session.getChannelId(), session);
        logger.info("added " + session);
    }

    public void removeSession(Session session) {
        removeSession(session.getChannel());
    }

    public void removeSession(Channel channel) {
        Session session = sessions.remove(channel.id());
        logger.info("removed " + session);
    }
}
