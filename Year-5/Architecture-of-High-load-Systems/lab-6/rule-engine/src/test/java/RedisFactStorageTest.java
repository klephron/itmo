import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;
import ru.itmo.zavar.redis.RedisFactStorage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

@Tag("unit")
public class RedisFactStorageTest {

    @Test
    void testAddFactAndGetFacts() {
        RedisCommands<String, String> mockRedis = mock(RedisCommands.class);
        RedisFactStorage storage = new RedisFactStorage(mockRedis, 5);

        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(1L, 65.0, 20.0, 123456789L, 10L);

        storage.addFact(payload);

        verify(mockRedis).eval(anyString(),
                eq(ScriptOutputType.STATUS),
                aryEq(new String[]{"radiator:1"}),
                eq("123456789"),
                eq("1:20.0:65.0:123456789"), // deviceId:roomTemp:radiatorTemp:timestamp
                eq("5"));

        when(mockRedis.zrange("radiator:1", 0, -1)).thenReturn(List.of("1:20.0:65.0:123456789"));

        List<String[]> result = storage.getFacts("radiator:1");
        assertEquals(1, result.size());
        assertArrayEquals(new String[]{"1","20.0","65.0","123456789"}, result.get(0));
    }
}
