import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.bson.Document;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.mqtt.IotMqttCallback;
import ru.itmo.zavar.payload.RadiatorMongoData;
import ru.itmo.zavar.payload.RadiatorMqttPayload;
import ru.itmo.zavar.task.MongoDBConnectTask;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

public class IotMqttCallbackTest {

    @Test
    void testValidMessageStoresToMongoAndPublishesToRabbit() throws Exception {
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        MongoDBConnectTask mongoTask = mock(MongoDBConnectTask.class);
        MongoClient mongoClient = mock(MongoClient.class);
        MongoDatabase database = mock(MongoDatabase.class);
        MongoCollection<RadiatorMongoData> collection = mock(MongoCollection.class);

        when(mongoTask.getMongoClient()).thenReturn(mongoClient);
        when(mongoTask.isTestingMode()).thenReturn(true);
        when(mongoClient.getDatabase("iot-db")).thenReturn(database);
        when(database.getCollection("rules", RadiatorMongoData.class)).thenReturn(collection);
        when(database.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));

        IotMqttCallback callback = new IotMqttCallback(channel, connection, "queue", mongoTask, "iot-db", "rules", () -> {});
        RadiatorMqttPayload payload = new RadiatorMqttPayload(1L, 60.0, 22.0, System.currentTimeMillis(), 10L);
        String json = new Gson().toJson(payload);

        MqttMessage mqttMessage = new MqttMessage(json.getBytes());
        callback.messageArrived("radiator/data", mqttMessage);

        verify(collection).insertOne(any(RadiatorMongoData.class));
        verify(channel).basicPublish(eq(""), eq("queue"), isNull(), any());
    }

    @Test
    void testInvalidMessageIsNotStored() throws Exception {
        Channel channel = mock(Channel.class);
        Connection connection = mock(Connection.class);
        MongoDBConnectTask mongoTask = mock(MongoDBConnectTask.class);

        IotMqttCallback callback = new IotMqttCallback(channel, connection, "queue", mongoTask, "iot-db", "rules", () -> {});
        // invalid radiatorTemp = 200
        RadiatorMqttPayload payload = new RadiatorMqttPayload(1L, 200.0, 22.0, System.currentTimeMillis(), 10L);
        String json = new Gson().toJson(payload);

        MqttMessage mqttMessage = new MqttMessage(json.getBytes());
        callback.messageArrived("radiator/data", mqttMessage);

        verify(mongoTask, never()).getMongoClient();
        verify(channel, never()).basicPublish(any(), any(), any(), any());
    }
}
