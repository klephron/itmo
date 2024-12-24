package ru.itmo.zavar;

public class Main {
    public static void main(String[] args) {
        World world = new World();
        world.offices.add(new PostOffice(new Address(new PostalAddress("Russia", "SPb", "188032"), "S1", "12")));
        // world.offices.add(new PostOffice(new Address(new PostalAddress("Russia",
        // "SPb", "188034"), "S2", "13")));
        world.actors.add(new IndividualActor(new Address(new PostalAddress("Russia", "SPb", "188032"), "S3", "14"),
                "A1", "B1", "C1"));
    }
}
