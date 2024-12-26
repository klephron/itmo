package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@Getter
@ToString
public class Transport {
    private final double speed; // units in second
    private List<Mail> mails;

    // Navigation
    private Route<PostOffice> route;
    private int routeNextIndex;
    private Coordinates prevCoordinates;
    private Coordinates nextCoordinates;
    private double prevNanos;

    Sinks.Many<Integer> sink = null;
    Disposable sinkFluxDisposable = null;

    public Transport(int size, double speed, Coordinates latestCoordinates) {
        mails = Arrays.asList(new Mail[size]);
        this.speed = speed;
        this.route = null;
        this.routeNextIndex = 0;
        this.prevCoordinates = latestCoordinates;
        this.nextCoordinates = latestCoordinates;
        this.prevNanos = System.nanoTime();
    }

    public Transport(int size, double speed) {
        this(size, speed, new Coordinates(0., 0.));
    }

    public Transport() {
        this(100, 10D);
    }

    // if new route -> start from first element
    // if already existed root need to:
    // 1. stop previous root
    // 2. recalculate latest coordinates
    // 3. start from beginning

    public void setRoute(Route<PostOffice> route) {
        this.route = route;
        this.routeNextIndex = 0;
    }

    public void start() {
        start(this.routeNextIndex);
    }

    public void start(int index) {
        if (this.route == null) {
            logger.warning("no route for transport to start");
            return;
        }
        if (this.sink != null) {
            logger.warning("transport already started");
            return;
        }

        this.routeNextIndex = index;
        this.sink = Sinks.many().unicast().onBackpressureBuffer();

        sinkFluxDisposable = sink.asFlux()
                .flatMap(counter -> {
                    PostOffice postOffice = route.getStops().get(this.routeNextIndex);
                    double distance = Coordinates.getDistance(this.prevCoordinates, postOffice.getCoordinates());
                    double delaySeconds = distance / this.speed;
                    Duration delay = Duration.ofMillis((long) (delaySeconds * 1000));

                    // here transport is "moving"
                    this.prevNanos = System.nanoTime();
                    this.nextCoordinates = postOffice.getCoordinates();
                    return Mono.delay(delay).map(tick -> counter);
                })
                .subscribe(counter -> this.visitPostOffice(counter));

        logger.fine("started " + this.toString());
        sink.emitNext(0, EmitFailureHandler.FAIL_FAST);
    }

    // calculate position based on prevCoordinates, nextCoordinates and speed
    public void stop() {
        if (this.sink == null) {
            return;
        }
        this.sink.emitComplete(EmitFailureHandler.FAIL_FAST);
        this.sinkFluxDisposable.dispose();
        this.sink = null;

        long currNano = System.nanoTime();
        double seconds = ((double) currNano - this.prevNanos) / 1_000_000_000;
        double distance = Coordinates.getDistance(this.prevCoordinates, this.nextCoordinates);
        double secondsRequired = (distance / this.speed);

        if (secondsRequired <= seconds) {
            this.prevCoordinates = this.nextCoordinates;
        } else {
            Coordinates unit = Coordinates.getUnitVector(this.prevCoordinates, this.nextCoordinates);
            this.prevCoordinates = new Coordinates(this.prevCoordinates.x() + unit.x() * seconds,
                    this.prevCoordinates.y() + unit.y() * seconds);
            this.nextCoordinates = this.prevCoordinates;
        }
        this.prevNanos = currNano;
        logger.fine("stopped " + this.toString());
    }

    private void visitPostOffice(Integer counter) {
        if (this.sink == null) {
            return;
        }

        this.prevNanos = System.nanoTime();
        this.prevCoordinates = this.nextCoordinates;

        // process Post Office
        PostOffice postOffice = route.getStops().get(this.routeNextIndex);
        Mono<Void> signal = postOffice.transportDeliver(this);

        // handle on error or complete
        signal.subscribe(v -> {
        }, e -> {
            logger.warning(e.toString());
            headFurther(counter, this.routeNextIndex);
        }, () -> {
            logger.fine("handled " + this.toString() + " at " + postOffice.getClass().getName() + " with "
                    + postOffice.getAddress());
            headFurther(counter, this.routeNextIndex);
        });
    }

    private void headFurther(Integer counter, int index) {
        Integer nextIndex = route.getNext(index);
        if (nextIndex == null) {
            sink.emitComplete(EmitFailureHandler.FAIL_FAST);
        } else {
            this.routeNextIndex = nextIndex;
            sink.emitNext(++counter, EmitFailureHandler.FAIL_FAST);
        }
    }
}
