package ru.itmo.zavar.mail;

import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.settings.FeedFormatType;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.mail.mark.MarkNominal;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

public final class MailGenerator {

    public Set<Mail> generateMails(int count) {
        Model<Mail> mailModel = Instancio.of(Mail.class)
                .subtype(field(Mail::getSender), IndividualActor.class)
                .subtype(field(Mail::getReceiver), IndividualActor.class)
                .ignore(field(Mail::getCreatedAt))
                .ignore(field(Mail::getSentAt))
                .ignore(field(Mail::getReceivedAt))
                .ignore(field(Mail::getMarks))
                .generate(field(Mail::getSubject), generators -> {
                    try {
                        return generators.oneOf(Files.readAllLines(Paths.get(ClassLoader.getSystemResource("ru/itmo/zavar/realistic_letter_subjects.csv").toURI())));
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onComplete(all(Mail.class), (Mail m) -> {
                    int value = m.getValue();
                    List<Integer> sortedNominals = MarkNominal.toValuesList();
                    ArrayList<Mark> result = new ArrayList<>();

                    for (int nominal : sortedNominals) {
                        while (value >= nominal) {
                            value -= nominal;
                            Mark mark = Instancio.of(Mark.class)
                                    .set(field(Mark::markNominal), MarkNominal.valueOf("N" + nominal))
                                    .create();
                            result.add(mark);
                        }
                    }

                    m.applyMarks(result);

                    if (m.getStatus().equals(MailStatus.CREATED)) {
                        m.setCreatedAt(Instancio.gen().temporal().localDateTime().get());
                    }
                    if (m.getStatus().equals(MailStatus.SENT)) {
                        m.setCreatedAt(Instancio.gen().temporal().localDateTime().get());
                        m.setSentAt(m.getCreatedAt().plusDays(Instancio.gen().longs().range(3L, 10L).get()));
                    }
                    if (m.getStatus().equals(MailStatus.RECEIVED)) {
                        m.setCreatedAt(Instancio.gen().temporal().localDateTime().get());
                        m.setSentAt(m.getCreatedAt().plusDays(Instancio.gen().longs().range(3L, 10L).get()));
                        m.setReceivedAt(m.getSentAt().plusDays(Instancio.gen().longs().range(3L, 10L).get()));
                    }
                })
                .toModel();

        return Instancio.ofSet(mailModel).size(count).create();
    }
}
