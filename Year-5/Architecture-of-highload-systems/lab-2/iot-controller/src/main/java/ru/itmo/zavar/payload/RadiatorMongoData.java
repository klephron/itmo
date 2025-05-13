package ru.itmo.zavar.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Getter
@AllArgsConstructor
public final class RadiatorMongoData {
    @BsonId
    private String id;

    @BsonProperty("device_id")
    @Setter
    private long deviceId;

    @BsonProperty("radiatorTemp")
    @Setter
    private double radiatorTemp;

    @BsonProperty("roomTemp")
    @Setter
    private double roomTemp;

    @BsonProperty("timestamp")
    @Setter
    private long timestamp;

    @BsonProperty("heat")
    @Setter
    private long heat;
}
