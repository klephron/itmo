package ru.itmo.zavar.stats;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.Mail;
import ru.itmo.zavar.mail.MailStatus;
import ru.itmo.zavar.mail.mark.Mark;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class MailStatisticsReactive {
    public Observable<Long> countByStatus(MailStatus status, Set<Mail> mailSet, long delay) {
        return Observable.fromIterable(mailSet)
            .observeOn(Schedulers.computation())
            .filter(mail -> status.equals(mail.getStatus()))
            .delay(delay, TimeUnit.MILLISECONDS)
            .count()
            .toObservable();
    }

    public Observable<Long> countByMark(Mark mark, Set<Mail> mailSet, long delay) {
        return Observable.fromIterable(mailSet)
            .observeOn(Schedulers.computation())
            .filter(mail -> mail.getMarks().contains(mark))
            .delay(delay, TimeUnit.MILLISECONDS)
            .count()
            .toObservable();
    }

    public Observable<Long> countBySender(IndividualActor sender, Set<Mail> mailSet, long delay) {
        return Observable.fromIterable(mailSet)
            .observeOn(Schedulers.computation())
            .filter(mail -> sender.equals(mail.getSender()))
            .delay(delay, TimeUnit.MILLISECONDS)
            .count()
            .toObservable();
    }

    public Observable<Long> countByReceiver(IndividualActor receiver, Set<Mail> mailSet, long delay) {
        return Observable.fromIterable(mailSet)
            .observeOn(Schedulers.computation())
            .filter(mail -> receiver.equals(mail.getReceiver()))
            .delay(delay, TimeUnit.MILLISECONDS)
            .count()
            .toObservable();
    }

    public Flowable<Mail> countByReceiverWithBackpressure(IndividualActor receiver, Set<Mail> mailSet) {
        Flowable<Mail> mailFlowable = Flowable.create(emitter -> {
            try {
                for (Mail mail : mailSet) {
                    emitter.onNext(mail);
                }
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        }, BackpressureStrategy.BUFFER);
        return mailFlowable
            .observeOn(Schedulers.computation())
            .filter(mail -> mail.getReceiver().equals(receiver));
    }

    public static class CustomMailSubscriber implements Subscriber<Mail> {

        private Subscription subscription;
        private final AtomicLong processedCount = new AtomicLong();
        private Instant startTime;

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            this.startTime = Instant.now();
            subscription.request(100);
        }

        @Override
        public void onNext(Mail mail) {
            processedCount.incrementAndGet();

            if (processedCount.get() % 100 == 0) {
                subscription.request(100);
            }
        }

        @Override
        public void onError(Throwable t) {
            System.err.println("Произошла ошибка: " + t.getMessage());
        }

        @Override
        public void onComplete() {
            Instant endTime = Instant.now();
            long elapsedTime = Duration.between(startTime, endTime).toMillis();
            System.out.println("Обработка завершена.");
            System.out.println("Всего обработано писем, удовлетворяющих критерию: " + processedCount.get());
            System.out.println("Время обработки: " + elapsedTime + " мс");
        }
    }
}
