package ru.itmo.zavar;

import static ru.itmo.zavar.World.logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

@Getter
@ToString
public class Transport {
    private final int capacity;
    private final List<Mail> mails;
    private final double speed; // units in second
    private final String number;

    // should be updated before processing post office
    private final Set<PostalAddress> loadingAddresses; // which addresses transport loads
    private final Set<PostalAddress> deliveryAddresses; // which it delivers

    // Navigation
    private Route<PostOffice> route;
    private int routeLatestIndex;
    private int routeNextIndex;
    private Coordinates latestCoordinates;
    private Coordinates nextCoordinates;
    private double latestNanos;

    private Sinks.Many<Integer> sink;
    private Disposable sinkFluxDisposable;

    private PostOffice atPostOffice = null;

    public Transport(int capacity, double speed, String number, Coordinates latestCoordinates) {
        this.capacity = capacity;
        this.mails = new ArrayList<>(capacity);
        this.speed = speed;
        this.number = number;
        this.loadingAddresses = new HashSet<>();
        this.deliveryAddresses = new HashSet<>();
        this.route = null;
        this.routeLatestIndex = 0;
        this.routeNextIndex = 0;
        this.latestCoordinates = latestCoordinates;
        this.nextCoordinates = latestCoordinates;
        this.latestNanos = System.nanoTime();
        this.sink = null;
        this.sinkFluxDisposable = null;
        this.atPostOffice = null;
    }

    public Transport(int size, double speed, String number) {
        this(size, speed, number, new Coordinates(0., 0.));
    }

    // if new route -> start from first element
    // if already existed root need to:
    // 1. stop previous root
    // 2. recalculate latest coordinates
    // 3. start from beginning

    public void setRoute(Route<PostOffice> route) {
        this.route = route;
        this.routeNextIndex = 0;
        this.routeLatestIndex = 0;
        // add all except current
        this.loadingAddresses.addAll(route.getStops().stream().map(po -> po.getAddress().postalAddress()).toList());
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

        this.sinkFluxDisposable = sink.asFlux()
                .flatMap(counter -> {
                    PostOffice postOffice = route.getStops().get(this.routeNextIndex);
                    double distance = Coordinates.getDistance(this.latestCoordinates, postOffice.getCoordinates());
                    double delaySeconds = distance / this.speed;
                    Duration delay = Duration.ofMillis((long) (delaySeconds * 1000));

                    // here transport is "moving"
                    this.latestNanos = System.nanoTime();
                    this.nextCoordinates = postOffice.getCoordinates();
                    return Mono.delay(delay).map(tick -> counter);
                })
                .subscribe(counter -> {
                    updateLoadingDelivery();
                    this.visitPostOffice(counter);
                });

        logger.fine("started " + this.getNumber());
        this.sink.emitNext(0, EmitFailureHandler.FAIL_FAST);
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
        double seconds = ((double) currNano - this.latestNanos) / 1_000_000_000;
        double distance = Coordinates.getDistance(this.latestCoordinates, this.nextCoordinates);
        double secondsRequired = (distance / this.speed);

        if (secondsRequired <= seconds) {
            this.latestCoordinates = this.nextCoordinates;
            this.routeLatestIndex = this.routeNextIndex;
        } else {
            Coordinates unit = Coordinates.getUnitVector(this.latestCoordinates, this.nextCoordinates);
            this.latestCoordinates = new Coordinates(this.latestCoordinates.x() + unit.x() * seconds,
                    this.latestCoordinates.y() + unit.y() * seconds);
        }
        this.latestNanos = currNano;
        logger.fine("stopped " + this.toString());
    }

    // should be called before visiting post office
    private void updateLoadingDelivery() {
        if (route == null || route.getStops().isEmpty()) {
            return;
        }
        // [to->0, 1, from->2, 3, 4]
        for (int i = this.routeLatestIndex; i < this.routeNextIndex; ++i) {
            this.loadingAddresses.add(route.getStops().get(i).getAddress().postalAddress());
        }
        if (this.route.isCyclic() && this.routeLatestIndex > this.routeNextIndex) {
            for (int i = this.routeLatestIndex; i < this.route.getStops().size(); ++i) {
                this.loadingAddresses.add(route.getStops().get(i).getAddress().postalAddress());
            }
            for (int i = 0; i < this.routeNextIndex; ++i) {
                this.loadingAddresses.add(route.getStops().get(i).getAddress().postalAddress());
            }
        }
        this.loadingAddresses.remove(route.getStops().get(this.routeNextIndex).getAddress().postalAddress());

        this.deliveryAddresses.clear();
        this.deliveryAddresses.add(route.getStops().get(this.routeNextIndex).getAddress().postalAddress());
    }

    private void visitPostOffice(Integer counter) {
        if (this.sink == null) {
            return;
        }

        this.latestNanos = System.nanoTime();
        this.latestCoordinates = this.nextCoordinates;

        // process Post Office
        PostOffice postOffice = route.getStops().get(this.routeNextIndex);
        this.atPostOffice = postOffice;
        Mono<Void> signal = postOffice.transportDeliver(this);

        // handle on error or complete
        signal.subscribe(v -> {
        }, e -> {
            this.routeLatestIndex = this.routeNextIndex;
            logger.warning(e.toString());
            headFurther(counter);
        }, () -> {
            this.routeLatestIndex = this.routeNextIndex;
            // logger.fine("handled " + this.toString() + " at " +
            // postOffice.getClass().getName() + " with "
            // + postOffice.getAddress());
            headFurther(counter);
        });
    }

    private void headFurther(Integer counter) {
        this.atPostOffice = null;
        Integer nextIndex = route.getNext(this.routeLatestIndex);
        if (nextIndex == null) {
            sink.emitComplete(EmitFailureHandler.FAIL_FAST);
        } else {
            this.routeNextIndex = nextIndex;
            sink.emitNext(++counter, EmitFailureHandler.FAIL_FAST);
        }
    }
}
