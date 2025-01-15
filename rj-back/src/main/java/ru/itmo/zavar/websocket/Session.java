package ru.itmo.zavar.websocket;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import lombok.*;
import org.apache.commons.lang3.builder.ToStringExclude;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

import java.time.Duration;
import java.util.Collection;

import static ru.itmo.zavar.model.World.logger;

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
                        boolean hasNext = nextFunc.next(this, channel);
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
            public boolean next(Subscription subscription, Channel channel);
        }
    }

    @Getter
    public class WorldSubscription extends Subscription {
        private final Collection<Long> postOfficeIds;
        private final Collection<Long> transportIds;
        private final Collection<Long> actorIds;

        public WorldSubscription(Float ups, OnNextFunc nextFunc, Collection<Long> postOfficeIds, Collection<Long> transportIds,
                Collection<Long> actorIds) {
            super(ups, nextFunc);
            this.postOfficeIds = postOfficeIds;
            this.transportIds = transportIds;
            this.actorIds = actorIds;
        }
    }
}
