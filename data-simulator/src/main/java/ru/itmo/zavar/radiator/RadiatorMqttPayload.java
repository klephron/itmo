package ru.itmo.zavar.radiator;

import java.time.Instant;
import java.util.Random;

public record RadiatorMqttPayload(Long deviceId, double radiatorTemp, double roomTemp, long timestamp) {
    private static double generateRadiatorTemperature(final Random random, final double anomalyProbability) {
        double base = 65.0 + random.nextGaussian() * 3;
        if (random.nextDouble() < anomalyProbability) {
            base += 30 + random.nextDouble() * 10;
        }
        return base;
    }

    private static double generateRoomTemperature(final Random random, final double anomalyProbability) {
        double base = 21.0 + random.nextGaussian() * 2;
        if (random.nextDouble() < anomalyProbability) {
            double anomalyShift = (random.nextDouble() * 25) - 10;
            base += anomalyShift;
        }
        return base;
    }

    public static RadiatorMqttPayload createRandom(final long deviceIndex, final Random random, final double anomalyProbability) {
        double radiatorTemp = generateRadiatorTemperature(random, anomalyProbability);
        double roomTemp = generateRoomTemperature(random, anomalyProbability);
        long timestamp = Instant.now().getEpochSecond();
        return new RadiatorMqttPayload(deviceIndex, radiatorTemp, roomTemp, timestamp);
    }

    public static RadiatorMqttPayload create(long deviceId, double radiatorTemp, double roomTemp) {
        return new RadiatorMqttPayload(deviceId, radiatorTemp, roomTemp, Instant.now().toEpochMilli());
    }
}
