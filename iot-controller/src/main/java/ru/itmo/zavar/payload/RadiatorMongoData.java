package ru.itmo.zavar.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@AllArgsConstructor
public final class RadiatorMongoData {
    @BsonId
    @Getter
    private String id;

    @BsonProperty("device_id")
    @Getter
    @Setter
    private long deviceId;

    @BsonProperty("radiatorTemp")
    @Getter
    @Setter
    private double radiatorTemp;

    @BsonProperty("roomTemp")
    @Getter
    @Setter
    private double roomTemp;

    @BsonProperty("timestamp")
    @Getter
    @Setter
    private long timestamp;
}
