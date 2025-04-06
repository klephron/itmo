package ru.itmo.zavar.generatorV2;

import ru.itmo.zavar.radiator.RadiatorMqttPayload;

import java.util.Random;

public class ComplexRadiatorGenerator {
    private final Random random;
    private final double strangeDataProbability;
    private final double anomalyProbability;
    private final double normalRadiatorMean;
    private final double normalRoomMean;
    private final double radiatorStd;
    private final double roomStd;

    public ComplexRadiatorGenerator(Random random, double strangeDataProbability, double anomalyProbability) {
        this.random = random;
        this.strangeDataProbability = strangeDataProbability;
        this.anomalyProbability = anomalyProbability;
        this.normalRadiatorMean = 65.0;
        this.normalRoomMean = 21.0;
        this.radiatorStd = 3.0;
        this.roomStd = 2.0;
    }

    private double generateRadiatorTemperature() {
        double base = normalRadiatorMean + random.nextGaussian() * radiatorStd;

        if (random.nextDouble() < anomalyProbability) {
            base += 30 + random.nextDouble() * 10;
        }

        if (random.nextDouble() < strangeDataProbability) {
            if (random.nextBoolean()) {
                base = -10 - random.nextDouble() * 5;
            } else {
                base = 150 + random.nextDouble() * 50;
            }
        }

        return base;
    }

    private double generateRoomTemperature() {
        double base = normalRoomMean + random.nextGaussian() * roomStd;

        if (random.nextDouble() < anomalyProbability) {
            double anomalyShift = (random.nextDouble() * 25) - 10;
            base += anomalyShift;
        }

        if (random.nextDouble() < strangeDataProbability) {
            if (random.nextBoolean()) {
                base = -5 - random.nextDouble() * 5;
            } else {
                base = 45 + random.nextDouble() * 10;
            }
        }

        return base;
    }

    public RadiatorMqttPayload createPayload(long deviceId) {
        double radiatorTemp = generateRadiatorTemperature();
        double roomTemp = generateRoomTemperature();
        return RadiatorMqttPayload.create(deviceId, radiatorTemp, roomTemp);
    }
}
