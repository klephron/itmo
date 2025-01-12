package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

import static ru.itmo.zavar.World.logger;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Actor {
    @EqualsAndHashCode.Include
    @Setter
    private Long id;
    private final Address address;
    private final Schedule schedule;

    public Actor(Address address) {
        this.address = address;
        this.schedule = new Schedule();
    }

    @Getter
    public class Schedule {
        private Map<Long, Task> tasks = new HashMap<>();
        private long idCounter = 0;

        public ImmutablePair<Task, Flux<Mail>> addTaskMailSendFlux(Flux<Mail> flux) {
            var task = new MailSendFluxTask(flux);
            task.setId(++idCounter);
            tasks.put(task.getId(), task);
            return new ImmutablePair<Task, Flux<Mail>>(task, task.send());
        }

        public ImmutablePair<Task, Mono<Mail>> addTaskMailSendOne(Mail mail) {
            var task = new MailSendOneTask(mail);
            task.setId(++idCounter);
            tasks.put(task.getId(), task);
            return new ImmutablePair<Task, Mono<Mail>>(task, task.send());
        }

        public ImmutablePair<Task, Void> addTaskMailReceiveOne(Mono<Mail> mono, Consumer<? super Mail> onOne,
                Consumer<Throwable> onError) {
            var task = new MailReceiveOneTask(mono, onOne, onError);
            task.setId(++idCounter);
            tasks.put(task.getId(), task);
            task.receive();
            return new ImmutablePair<Task, Void>(task, null);
        }

        public ImmutablePair<Task, Void> addTaskMailReceiveFlux(Flux<Mail> flux, Consumer<? super Mail> onOne,
                Consumer<Throwable> onError) {
            var task = new MailReceiveFluxTask(flux, onOne, onError);
            task.setId(++idCounter);
            tasks.put(task.getId(), task);
            task.receive();
            return new ImmutablePair<Task, Void>(task, null);
        }

        public void deleteTask(long taskId) {
            if (!tasks.containsKey(taskId)) {
                return;
            }
            tasks.get(taskId).terminate();
        }

        @RequiredArgsConstructor
        @Getter
        public abstract class Task {
            @Setter
            private Long id;

            void onEnd() {
                logger.fine("task is done, removing");
                tasks.remove(this.getId());
                logger.fine("task is removed");
            }

            abstract void terminate();
        }

        public class MailReceiveOneTask extends Task {
            private final Mono<Mail> mono;
            private final Consumer<? super Mail> consumer;
            private final Consumer<? super Throwable> errorConsumer;
            private Disposable disposable = null;

            public MailReceiveOneTask(Mono<Mail> mono, Consumer<? super Mail> consumer,
                    Consumer<? super Throwable> errorConsumer) {
                this.mono = mono;
                this.consumer = consumer;
                this.errorConsumer = errorConsumer;
            }

            void receive() {
                disposable = mono.doOnSuccess(v -> {
                    this.onEnd();
                }).subscribe(consumer, errorConsumer);
            }

            @Override
            void terminate() {
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }

        public class MailReceiveFluxTask extends Task {
            private final Flux<Mail> flux;
            private final Consumer<? super Mail> consumer;
            private final Consumer<? super Throwable> errorConsumer;
            private Disposable disposable = null;

            public MailReceiveFluxTask(Flux<Mail> flux, Consumer<? super Mail> consumer,
                    Consumer<? super Throwable> errorConsumer) {
                this.flux = flux;
                this.consumer = consumer;
                this.errorConsumer = errorConsumer;
            }

            void receive() {
                disposable = flux.doOnTerminate(this::onEnd).subscribe(consumer, errorConsumer);
            }

            @Override
            void terminate() {
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }

        // NOTE: sink is hot stream, flux is cold, can't stop flux so some elements can
        // be lost while propagating down. To guarantee not to lose elements either pass
        // sink or anything that is not cold stream (even just function with callback +
        // delay, that emulates generator or generator with parameters itself)

        public class MailSendOneTask extends Task {
            private final Mail mail;
            private Sinks.One<Mail> sink;

            public MailSendOneTask(Mail mail) {
                this.mail = mail;
                this.sink = Sinks.one();
            }

            Mono<Mail> send() {
                sink.emitValue(mail, EmitFailureHandler.FAIL_FAST);
                return sink.asMono();
            }

            @Override
            void terminate() {
                sink.emitEmpty(EmitFailureHandler.FAIL_FAST);
            }
        }

        public class MailSendFluxTask extends Task {
            private final Flux<Mail> flux;
            private final Sinks.Many<Mail> sink;
            private Disposable disposable;

            public MailSendFluxTask(Flux<Mail> flux) {
                this.flux = flux;
                this.sink = Sinks.many().unicast().onBackpressureBuffer();
                this.disposable = null;
            }

            Flux<Mail> send() {
                disposable = flux.subscribe(mail -> {
                    sink.tryEmitNext(mail);
                }, error -> sink.tryEmitError(error), () -> sink.tryEmitComplete());
                return sink.asFlux();
            }

            @Override
            void terminate() {
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }
    }
}
