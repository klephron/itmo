package ru.itmo.zavar.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@RequiredArgsConstructor
public class Session {
    @EqualsAndHashCode.Include
    private final Channel channel;

    public ChannelId getChannelId() {
        return channel.id();
    }
}
