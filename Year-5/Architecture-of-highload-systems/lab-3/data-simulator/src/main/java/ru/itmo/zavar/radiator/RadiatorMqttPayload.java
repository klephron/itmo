package ru.itmo.zavar.radiator;

import java.time.Instant;
import java.util.Random;

public record RadiatorMqttPayload(Long deviceId, double radiatorTemp, double roomTemp, long timestamp, long heat) {
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

    public static RadiatorMqttPayload createRandom(final Long deviceId, final Random random, final double anomalyProbability, long heat) {
        double radiatorTemp = generateRadiatorTemperature(random, anomalyProbability);
        double roomTemp = generateRoomTemperature(random, anomalyProbability);
        long timestamp = Instant.now().getEpochSecond();
        return new RadiatorMqttPayload(deviceId, radiatorTemp, roomTemp, timestamp, heat);
    }

    public static RadiatorMqttPayload create(final Long deviceId, final double radiatorTemp, final double roomTemp, long heat) {
        return new RadiatorMqttPayload(deviceId, radiatorTemp, roomTemp, Instant.now().toEpochMilli(), heat);
    }
}
