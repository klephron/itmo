package ru.itmo.zavar.model;

public record Coordinates(double x, double y) {
    public static double getDistance(Coordinates lsv, Coordinates rsv) {
        return Math.sqrt((Math.pow(lsv.x() - rsv.x(), 2) + Math.pow(lsv.y() - rsv.y(), 2)));
    }

    public static Coordinates getUnitVector(Coordinates from, Coordinates to) {
        double distance = getDistance(from, to);
        return new Coordinates((to.x() - from.x()) / distance, (to.y() - from.y()) / distance);
    }
}
