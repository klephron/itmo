package ru.itmo.zavar.mail;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.generator.Generator;
import ru.itmo.zavar.actor.Address;
import ru.itmo.zavar.actor.IndividualActor;
import ru.itmo.zavar.mail.mark.Mark;
import ru.itmo.zavar.mail.mark.MarkNominal;

import java.io.Reader;
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

        List<String[]> addresses;
        List<String[]> actors;
        List<String> subjects;
        try {
            addresses = readAllLinesFromCsv(Paths.get(ClassLoader.getSystemResource("ru/itmo/zavar/realistic_addresses.csv").toURI()));
            actors = readAllLinesFromCsv(Paths.get(ClassLoader.getSystemResource("ru/itmo/zavar/realistic_names.csv").toURI()));
            subjects = Files.readAllLines(Paths.get(ClassLoader.getSystemResource("ru/itmo/zavar/realistic_letter_subjects.csv").toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Generator<Address> addressGenerator = random -> {
            String[] oneOf = random.oneOf(addresses);
            return new Address(oneOf[0], oneOf[1], oneOf[2], oneOf[3], oneOf[4]);
        };

        Generator<IndividualActor> individualActorGenerator = random -> {
            String[] oneOf = random.oneOf(actors);
            Address address = Instancio.of(Address.class).generate(all(Address.class), addressGenerator).create();
            return new IndividualActor(oneOf[0], oneOf[1], oneOf[2], address);
        };

        Model<Mail> mailModel = Instancio.of(Mail.class)
                .subtype(field(Mail::getSender), IndividualActor.class)
                .subtype(field(Mail::getReceiver), IndividualActor.class)
                .generate(field(Mail::getSender), individualActorGenerator)
                .generate(field(Mail::getReceiver), individualActorGenerator)
                .ignore(field(Mail::getCreatedAt))
                .ignore(field(Mail::getSentAt))
                .ignore(field(Mail::getReceivedAt))
                .ignore(field(Mail::getMarks))
                .generate(field(Mail::getSubject), generators -> generators.oneOf(subjects))
                .onComplete(all(Mail.class), (Mail m) -> {
                    long value = m.getValue();
                    List<Integer> sortedNominals = MarkNominal.toValuesList();
                    ArrayList<Mark> result = new ArrayList<>();

                    for (int nominal : sortedNominals) {
                        while (value >= nominal) {
                            value -= nominal;
                            Mark mark = Instancio.of(Mark.class)
                                    .set(field(Mark::markNominal), MarkNominal.valueOf("N" + nominal))
                                    .set(field(Mark::country), m.getSender().getAddress().country())
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

    private List<String[]> readAllLinesFromCsv(Path filePath) throws Exception {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build()) {
                return csvReader.readAll();
            }
        }
    }
}
