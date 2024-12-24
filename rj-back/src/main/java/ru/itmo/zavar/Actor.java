package ru.itmo.zavar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import reactor.core.publisher.Mono;

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

        public void addTaskMailReceive(Mono<Mail> mono, Consumer<? super Mail> consumer) {
            var task = new MailReceiveOneTask(mono, consumer);
            tasks.put(task, null);
            task.receive();
        }

        public Mono<Mail> addTaskMailSend(Mail mail) {
            var task = new MailSendOneTask(mail);
            tasks.put(task, null);
            return task.send();
        }

        public abstract class Task {
            void onSuccess() {
                logger.log(Level.INFO, "task is done, removing");
                tasks.remove(this);
                logger.log(Level.INFO, "task is removed");
            }

        }

        @RequiredArgsConstructor
        public class MailReceiveOneTask extends Task {
            private final Mono<Mail> mono;
            private final Consumer<? super Mail> consumer;

            void receive() {
                mono.doOnSuccess(v -> {
                    this.onSuccess();
                }).subscribe(consumer);
            }

        }

        @RequiredArgsConstructor
        public class MailSendOneTask extends Task {
            private final Mail mail;

            Mono<Mail> send() {
                return Mono.just(mail).doOnSuccess(v -> this.onSuccess());
            }
        }
    }
}
