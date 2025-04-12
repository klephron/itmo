import com.google.gson.Gson;
import com.rabbitmq.client.Delivery;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;
import ru.itmo.zavar.redis.RedisFactStorage;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.RabbitMQConsumeCallback;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.List;

@Tag("unit")
public class RabbitMQConsumeCallbackTest {

    @Test
    void testHandleValidMessage() throws IOException {
        RedisFactStorage mockStorage = mock(RedisFactStorage.class);
        RedisCommands<String, String> mockRedis = mock(RedisCommands.class);
        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);

        RabbitMQConsumeCallback callback = new RabbitMQConsumeCallback(
                mockStorage, mockRedis, mockMongo,
                "iot-db", "rules", "replica1"
        );

        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(1L, 70.0, 22.0, 123L, 5L);
        String json = new Gson().toJson(payload);

        Delivery delivery = mock(Delivery.class);
        when(delivery.getBody()).thenReturn(json.getBytes());

        when(mockRedis.keys("radiator:1")).thenReturn(List.of("radiator:1"));

        callback.handle("consumer-tag", delivery);

        verify(mockStorage).addFact(payload);
        verify(mockRedis).keys("radiator:1");
    }
}
