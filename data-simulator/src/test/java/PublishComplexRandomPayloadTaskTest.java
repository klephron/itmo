import com.google.gson.Gson;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.itmo.zavar.radiator.RadiatorMqttPayload;
import ru.itmo.zavar.task.PublishComplexRandomPayloadTask;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PublishComplexRandomPayloadTaskTest {

    private MqttClient mockClient;
    private Random mockRandom;
    private long[] prevHeat;
    private PublishComplexRandomPayloadTask task;

    @BeforeEach
    void setUp() {
        mockClient = mock(MqttClient.class);
        mockRandom = mock(Random.class);

        prevHeat = new long[]{0};
        task = new PublishComplexRandomPayloadTask(
                mockRandom,
                1,
                0.0,
                0.0,
                0.0,
                "test/topic",
                "test/reset",
                mockClient,
                prevHeat
        );

        when(mockRandom.nextDouble()).thenReturn(0.5);
        when(mockRandom.nextGaussian()).thenReturn(0.0);
    }

    @Test
    void testRunPublishesMessage() throws MqttException {
        task.run();

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mockClient, times(1)).publish(eq("test/topic"), captor.capture());

        MqttMessage message = captor.getValue();
        String payloadJson = new String(message.getPayload());

        RadiatorMqttPayload payload = new Gson().fromJson(payloadJson, RadiatorMqttPayload.class);
        assertEquals(1L, payload.deviceId());
        assertEquals(1, payload.heat());
        assertEquals(65.0, payload.radiatorTemp(), 0.01);
        assertEquals(21.0, payload.roomTemp(), 0.01);
    }

    @Test
    void testSkipProbabilityPreventsPublishing() throws MqttException {
        when(mockRandom.nextDouble()).thenReturn(0.5, 0.05);

        task = new PublishComplexRandomPayloadTask(
                mockRandom,
                1,
                0.1,
                0.0,
                0.0,
                "test/topic",
                "test/reset",
                mockClient,
                prevHeat
        );

        task.run();

        verify(mockClient, never()).publish(anyString(), any());
    }

    @Test
    void testResetClearsHeat() {
        prevHeat[0] = 10;
        task.reset();
        task.run();
        assertEquals(1, prevHeat[0]);
    }

    @Test
    void testDeviceGoesOffline() {
        when(mockRandom.nextDouble()).thenReturn(0.005);

        task.run();
        task.run();

        try {
            verify(mockClient, never()).publish(anyString(), any());
        } catch (MqttException e) {
            fail("MqttException should not be thrown");
        }
    }
}

