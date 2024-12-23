package ru.itmo.zavar;

public class Main {
    public static void main(String[] args) {
        World world = new World();
        world.offices.add(new PostOffice(10., 20.));
        world.offices.add(new PostOffice(20., 30.));
        world.transports.add(new Transport());
    }
}
