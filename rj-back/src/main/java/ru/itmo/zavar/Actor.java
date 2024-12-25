package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.ImmutablePair;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.EmitFailureHandler;

import static ru.itmo.zavar.World.logger;

@AllArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
public abstract class Actor {
    private final Address address;

    @Getter
    public class Schedule {
        private Map<Task, Void> tasks = new HashMap<>();

        public ImmutablePair<Task, Flux<Mail>> addTaskMailSendFlux(Flux<Mail> flux) {
            var task = new MailSendFluxTask(flux);
            tasks.put(task, null);
            return new ImmutablePair<Task, Flux<Mail>>(task, task.send());
        }

        public ImmutablePair<Task, Mono<Mail>> addTaskMailSendOne(Mail mail) {
            var task = new MailSendOneTask(mail);
            tasks.put(task, null);
            return new ImmutablePair<Task, Mono<Mail>>(task, task.send());
        }

        public ImmutablePair<Task, Void> addTaskMailReceiveOne(Mono<Mail> mono, Consumer<? super Mail> onOne,
                Consumer<Throwable> onError) {
            var task = new MailReceiveOneTask(mono, onOne, onError);
            tasks.put(task, null);
            task.receive();
            return new ImmutablePair<Task, Void>(task, null);
        }

        public ImmutablePair<Task, Void> addTaskMailReceiveFlux(Flux<Mail> flux, Consumer<? super Mail> onOne,
                Consumer<Throwable> onError) {
            var task = new MailReceiveFluxTask(flux, onOne, onError);
            tasks.put(task, null);
            task.receive();
            return new ImmutablePair<Task, Void>(task, null);
        }

        public void deleteTask(Task task) {
            if (!tasks.containsKey(task)) {
                return;
            }
            task.terminate();
        }

        @RequiredArgsConstructor
        public abstract class Task {
            void onEnd() {
                logger.fine("task is done, removing");
                tasks.remove(this);
                logger.fine("task is removed");
            }

            abstract void terminate();
        }

        @RequiredArgsConstructor
        public class MailReceiveOneTask extends Task {
            private final Mono<Mail> mono;
            private final Consumer<? super Mail> consumer;
            private final Consumer<? super Throwable> errorConsumer;
            private Disposable disposable = null;

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

        @RequiredArgsConstructor
        public class MailReceiveFluxTask extends Task {
            private final Flux<Mail> flux;
            private final Consumer<? super Mail> consumer;
            private final Consumer<? super Throwable> errorConsumer;
            private Disposable disposable = null;

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

        @RequiredArgsConstructor
        public class MailSendOneTask extends Task {
            private final Mail mail;
            private Sinks.One<Mail> sink = Sinks.one();

            Mono<Mail> send() {
                sink.emitValue(mail, EmitFailureHandler.FAIL_FAST);
                return sink.asMono();
            }

            @Override
            void terminate() {
                sink.emitEmpty(EmitFailureHandler.FAIL_FAST);
            }
        }

        @RequiredArgsConstructor
        public class MailSendFluxTask extends Task {
            private final Flux<Mail> flux;
            private Sinks.Many<Mail> sink = Sinks.many().unicast().onBackpressureBuffer();
            private Disposable disposable = null;

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
