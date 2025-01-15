package ru.itmo.zavar.websocket.pack;

import ru.itmo.zavar.model.Coordinates;
import ru.itmo.zavar.model.Mail;
import ru.itmo.zavar.model.Mail.Mark;
import ru.itmo.zavar.model.Postoffice;
import ru.itmo.zavar.model.Transport;
import ru.itmo.zavar.model.actor.Actor;
import ru.itmo.zavar.model.actor.Actor.Schedule;
import ru.itmo.zavar.model.actor.Actor.Schedule.Task;
import ru.itmo.zavar.model.actor.IndividualActor;
import ru.itmo.zavar.model.actor.OrganizationActor;
import ru.itmo.zavar.model.address.Address;
import ru.itmo.zavar.model.address.PostalAddress;

import java.util.LinkedList;
import java.util.List;

import static ru.itmo.zavar.model.World.logger;

public class PackMappers {
    public static PostofficePack postOfficeToPack(Postoffice office) {
        List<MailPack> terminalMails = office.getTerminalMails().values().stream()
                .flatMap(d -> d.stream().map(PackMappers::mailToPack)).toList();
        List<MailPack> intermediateMails = office.getIntermediateMails().values().stream()
                .flatMap(d -> d.stream().map(PackMappers::mailToPack)).toList();
        return new PostofficePack(office.getId(), addressToPack(office.getAddress()),
                coordinatesToPack(office.getCoordinates()), terminalMails, intermediateMails,
                office.getTerminalMailsCount(), office.getIntermediateMailsCount(), office.getTransportsQueued(),
                office.getTransportsProcessed());
    }

    public static PostofficePack PostOfficeToPackEssential(Postoffice office) {
        return new PostofficePack(office.getId(), null, coordinatesToPack(office.getCoordinates()), null, null,
                office.getTerminalMailsCount(), office.getIntermediateMailsCount(), office.getTransportsQueued(),
                office.getTransportsProcessed());
    }

    public static Postoffice postOfficeFromPack(PostofficePack office) {
        return new Postoffice(addressFromPack(office.getAddress()), coordinatesFromPack(office.getCoordinates()));
    }

    public static AddressPack addressToPack(Address address) {
        return new AddressPack(address.postalAddress().country(), address.postalAddress().city(),
                address.postalAddress().postalCode(), address.street(), address.house());
    }

    public static Address addressFromPack(AddressPack address) {
        return new Address(new PostalAddress(address.getCountry(), address.getCity(), address.getPostalCode()),
                address.getStreet(), address.getHouse());
    }

    public static CoordinatesPack coordinatesToPack(Coordinates coordinates) {
        return new CoordinatesPack(coordinates.x(), coordinates.y());
    }

    public static Coordinates coordinatesFromPack(CoordinatesPack coordinates) {
        double x;
        if (coordinates.getX() == null) {
            logger.warning("CoordinatesPack x is null, set to 0");
            x = 0;
        } else {
            x = coordinates.getX().doubleValue();
        }
        double y;
        if (coordinates.getY() == null) {
            logger.warning("CoordinatesPack y is null, set to 0");
            y = 0;
        } else {
            y = coordinates.getY().doubleValue();
        }
        return new Coordinates(x, y);
    }

    public static MailPack mailToPack(Mail mail) {
        return new MailPack(mail.getId(), mail.getSubject(), mail.getSender().getId(), mail.getReceiver().getId(),
                mail.getMailData().text(), mail.getValue(),
                mail.getMarks() == null ? null : mail.getMarks().stream().map(PackMappers::markToPack).toList(),
                mailStatusToPack(mail.getStatus()),
                mail.getCreatedAt() == null ? null : mail.getCreatedAt().toString(),
                mail.getAppliedAt() == null ? null : mail.getAppliedAt().toString(),
                mail.getSentAt() == null ? null : mail.getSentAt().toString(),
                mail.getReceivedAt() == null ? null : mail.getReceivedAt().toString());
    }

    public static Mail mailFromPack(MailPack mail, Actor sender, Actor receiver) {
        long id;
        if (mail.getId() == null) {
            logger.warning("mailpack id is null, set to 0");
            id = 0;
        } else {
            id = mail.getId().longValue();
        }

        long value;
        if (mail.getValue() == null) {
            logger.warning("mailpack value is null, set to 0");
            value = 0;
        } else {
            value = mail.getValue().longValue();
        }
        return new Mail(id, mail.getSubject(), sender, receiver, new Mail.Data(mail.getText()), value);
    }

    public static MarkPack markToPack(Mark mark) {
        return new MarkPack(nominalToPack(mark.markNominal()), mark.country(), mark.name());
    }

    public static Mark markFromPack(MarkPack mark) {
        return new Mark(nominalFromPack(mark.getNominal()), mark.getCountry(), mark.getName());
    }

    public static Mark.Nominal nominalFromPack(Integer nominal) {
        switch (nominal) {
            case 1:
                return Mark.Nominal.N1;
            case 10:
                return Mark.Nominal.N10;
            case 20:
                return Mark.Nominal.N20;
            case 25:
                return Mark.Nominal.N25;
            case 50:
                return Mark.Nominal.N50;
            case 100:
                return Mark.Nominal.N100;
            case 150:
                return Mark.Nominal.N150;
            case 200:
                return Mark.Nominal.N200;
            case 250:
                return Mark.Nominal.N250;
            case 500:
                return Mark.Nominal.N500;
            default:
                logger.warning("mark value is unknown '" + nominal + "'" + ", set to 1");
                return Mark.Nominal.N1;
        }
    }

    public static Integer nominalToPack(Mark.Nominal nominal) {
        if (nominal == null) {
            return 0;
        }
        return nominal.getValue();
    }

    public static MailPack.Status mailStatusToPack(Mail.Status status) {
        switch (status) {
            case APPLIED:
                return MailPack.Status.APPLIED;
            case CREATED:
                return MailPack.Status.CREATED;
            case RECEIVED:
                return MailPack.Status.RECEIVED;
            case SENT:
                return MailPack.Status.SENT;
            default:
                logger.warning("mail status is unknown '" + status + "'" + ", set to APPLIED");
                return MailPack.Status.APPLIED;
        }
    }

    public static TransportPack transportToPack(Transport transport) {
        List<Long> route;
        if (transport.getRoute() == null) {
            route = null;
        } else {
            route = transport.getRoute().getStops().stream().map(p -> p.getId()).toList();
        }

        List<PostalAddressPack> loadingAddresses;
        if (transport.getLoadingAddresses() == null) {
            loadingAddresses = new LinkedList<>();
        }
        loadingAddresses = transport.getLoadingAddresses().stream().map(PackMappers::postalAddressToPack).toList();

        List<PostalAddressPack> deliveryAddresses;
        if (transport.getDeliveryAddresses() == null) {
            deliveryAddresses = new LinkedList<>();
        }
        deliveryAddresses = transport.getDeliveryAddresses().stream().map(PackMappers::postalAddressToPack).toList();

        RoutePack routePack = new RoutePack(route, transport.getRoute() != null && transport.getRoute().isCyclic());

        return new TransportPack(transport.getId(), transport.getCapacity(), transport.getSpeed(), transport.getOps(),
                transport.getLicensePlate(), transport.getMails().stream().map(PackMappers::mailToPack).toList(),
                transport.getMails().size(), routePack,
                loadingAddresses, deliveryAddresses, coordinatesToPack(transport.getEstimatedCoordinates()),
                transport.getAtPostOffice() == null ? null : transport.getAtPostOffice().getId());
    }

    public static TransportPack transportToPackEssential(Transport transport) {
        return new TransportPack(transport.getId(), null, null, null, null, null, transport.getMails().size(), null,
                null, null, coordinatesToPack(transport.getEstimatedCoordinates()),
                transport.getAtPostOffice() == null ? null : transport.getAtPostOffice().getId());
    }

    public static Transport transportFromPack(TransportPack transport) {
        int capacity;
        if (transport.getCapacity() == null) {
            logger.warning("TransportPack capacity is null, set to 10");
            capacity = 10;
        } else {
            capacity = transport.getCapacity().intValue();
        }

        double speed;
        if (transport.getSpeed() == null) {
            logger.warning("TransportPack speed is null, set to 1");
            speed = 1;
        } else {
            speed = transport.getSpeed().intValue();
        }

        double ops;
        if (transport.getOps() == null) {
            logger.warning("TransportPack ops is null, set to 10");
            ops = 10;
        } else {
            ops = transport.getOps().intValue();
        }

        return new Transport(capacity, speed, ops,
                transport.getLicensePlate(), coordinatesFromPack(transport.getCoordinates()));
    }

    public static PostalAddressPack postalAddressToPack(PostalAddress postalAddress) {
        return new PostalAddressPack(postalAddress.country(), postalAddress.city(), postalAddress.postalCode());
    }

    public static PostalAddress postalAddressFromPack(PostalAddressPack postalAddress) {
        return new PostalAddress(postalAddress.getCountry(), postalAddress.getCity(), postalAddress.getPostalCode());
    }

    public static ActorPack actorToPack(Actor actor) {
        List<TaskPack> tasks = actor.getSchedule().getTasks().values().stream().map(PackMappers::taskToPack).toList();
        if (actor instanceof IndividualActor individualActor) {
            List<MailPack> mailPacks = actor.getAllMailsInBuffer().stream().map(PackMappers::mailToPack).toList();
            return new IndividualActorPack(individualActor.getId(), addressToPack(individualActor.getAddress()), tasks,
                mailPacks, individualActor.getBufferCapacity(), individualActor.getBufferCurrentSize(),
                individualActor.getMailsCount().get(), individualActor.getName(), individualActor.getSurname(),
                individualActor.getPatronymic());
        }
        if (actor instanceof OrganizationActor organizationActor) {
            List<MailPack> mailPacks = actor.getAllMailsInBuffer().stream().map(PackMappers::mailToPack).toList();
            return new OrganizationActorPack(organizationActor.getId(), addressToPack(organizationActor.getAddress()),
                    tasks, organizationActor.getBufferCapacity(), organizationActor.getBufferCurrentSize(),
                organizationActor.getMailsCount().get(), mailPacks, organizationActor.getName());
        }
        logger.warning("unhandled actor class " + actor);
        return new ActorPack(null, actor.getId(), addressToPack(actor.getAddress()), tasks, 0, 0 , 0, List.of());
    }

    public static ActorPack actorToPackEssential(Actor actor) {
        if (actor instanceof IndividualActor individualActor) {
            return new IndividualActorPack(individualActor.getId(), null, null, List.of(), 0, 0, 0,null, null, null);
        }
        if (actor instanceof OrganizationActor organizationActor) {
            return new OrganizationActorPack(organizationActor.getId(), null, null, 0, 0, 0, List.of(), null);
        }
        logger.warning("unhandled actor class " + actor);
        return new ActorPack(null, actor.getId(), null, null, 0, 0, 0, List.of());
    }

    public static Actor actorFromPack(ActorPack actor) {
        return switch (actor.getKind()) {
            case INDIVIDUAL -> {
                var casted = (IndividualActorPack) actor;
                yield new IndividualActor(addressFromPack(casted.getAddress()), casted.getBufferCapacity(),
                    casted.getName(), casted.getSurname(), casted.getPatronymic());
            }
            case ORGANIZATION -> {
                var casted = (OrganizationActorPack) actor;
                yield new OrganizationActor(addressFromPack(casted.getAddress()), casted.getBufferCapacity(), casted.getName());
            }
            default -> {
                logger.warning("unhandled actor class " + actor + ", default to INDIVIDUAL");
                yield new IndividualActor(addressFromPack(actor.getAddress()), 0, null, null, null);
            }
        };
    }

    // TODO: add additional metadata (currently only id and kind available) + marks
    public static TaskPack taskToPack(Task task) {
        if (task instanceof Schedule.MailSendOneTask casted) {
            return new TaskPack.SendMailOne(casted.getId(), casted.getOwnerActor().getId(), null, null);
        }
        if (task instanceof Schedule.MailSendFluxTask casted) {
            return new TaskPack.SendMailRepeated(casted.getId(), casted.getOwnerActor().getId(), null, null, null);
        }
        if (task instanceof Schedule.MailReceiveOneTask casted) {
            return new TaskPack(casted.getId(), TaskPack.Kind.RECEIVE_MAIL_ONE, null);
        }
        if (task instanceof Schedule.MailReceiveFluxTask casted) {
            return new TaskPack(casted.getId(), TaskPack.Kind.RECEIVE_MAIL_MANY, null);
        }
        return new TaskPack(task.getId(), null, null);
    }
}
