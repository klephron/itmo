package ru.itmo.zavar.websocket;

import static ru.itmo.zavar.World.logger;

import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringExclude;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@ToString
@Getter
@RequiredArgsConstructor
public class Session {
    @EqualsAndHashCode.Include
    private final Channel channel;

    @Setter
    private WorldSubscription subscription;

    public ChannelId getChannelId() {
        return channel.id();
    }

    @RequiredArgsConstructor
    @Getter
    public abstract class Subscription {
        private final Float ups;
        private final OnNextFunc nextFunc;

        @ToStringExclude
        private Sinks.Many<Boolean> sink;
        @ToStringExclude
        private Disposable sinkFluxDisposable;
        private long latestUpdateNanos;

        public void start() {
            this.sink = Sinks.many().unicast().onBackpressureBuffer();
            this.sinkFluxDisposable = sink.asFlux()
                    .flatMap(w -> {
                        // calculate next delay based on previous update
                        long nextUpdateNanos = this.latestUpdateNanos + (long) (1. / ups * 1_000_000_000);
                        long delayNanos = nextUpdateNanos - System.nanoTime();
                        delayNanos = delayNanos < 0 ? 0 : delayNanos;
                        Duration delay = Duration.ofNanos(delayNanos);

                        this.latestUpdateNanos = nextUpdateNanos;
                        return Mono.delay(delay).map(_ -> w);
                    }).subscribe(w -> {
                        if (!channel.isActive()) {
                            logger.info("channel is not active, cancelling subscription");
                            this.cancel();
                            return;
                        }
                        boolean hasNext = nextFunc.next(channel);
                        if (hasNext) {
                            this.sink.emitNext(w, EmitFailureHandler.FAIL_FAST);
                        } else {
                            this.cancel();
                        }
                    });

            this.latestUpdateNanos = System.nanoTime() - (long) (1. / ups * 1_000_000_000);
            this.sink.emitNext(true, EmitFailureHandler.FAIL_FAST);
        }

        public void cancel() {
            if (this.sink == null) {
                return;
            }
            this.sink.emitComplete(EmitFailureHandler.FAIL_FAST);
            this.sinkFluxDisposable.dispose();
            this.sink = null;
        }

        public boolean isSubscribed() {
            return this.sink != null;
        }

        public static interface OnNextFunc {
            public boolean next(Channel channel);
        }
    }

    @Getter
    public class WorldSubscription extends Subscription {
        private final List<Long> postOfficeIds;
        private final List<Long> transportIds;
        private final List<Long> actorIds;

        public WorldSubscription(Float ups, OnNextFunc nextFunc, List<Long> postOfficeIds, List<Long> transportIds,
                List<Long> actorIds) {
            super(ups, nextFunc);
            this.postOfficeIds = postOfficeIds;
            this.transportIds = transportIds;
            this.actorIds = actorIds;
        }
    }
}
