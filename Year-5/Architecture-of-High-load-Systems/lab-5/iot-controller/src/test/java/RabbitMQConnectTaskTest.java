import com.rabbitmq.client.*;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.task.RabbitMQConnectTask;
import ru.itmo.zavar.task.exithandler.DefaultExitHandler;
import ru.itmo.zavar.task.exithandler.ExitHandler;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class RabbitMQConnectTaskTest {

    @Test
    void testSuccessfulRabbitMQConnection() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class, withSettings().extraInterfaces(Recoverable.class));
        Channel channel = mock(Channel.class);
        MqttClient mqttClient = mock(MqttClient.class);

        when(factory.newConnection()).thenReturn(connection);
        when(connection.createChannel()).thenReturn(channel);

        RabbitMQConnectTask task = new RabbitMQConnectTask(factory, executor, mqttClient, "testQueue", 1, new DefaultExitHandler());
        task.run();

        assertEquals(connection, task.getConnection());
        assertEquals(channel, task.getChannel());
        verify(channel).queueDeclare(eq("testQueue"), eq(false), eq(false), eq(false), isNull());
        verify(executor).shutdown();
    }

    @Test
    void testFailedRabbitMQConnection() throws Exception {
        ExecutorService executor = mock(ExecutorService.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);
        MqttClient mqttClient = mock(MqttClient.class);
        ExitHandler exitHandler = mock(ExitHandler.class);

        when(factory.newConnection()).thenThrow(new RuntimeException("Connection failed"));

        RabbitMQConnectTask task = new RabbitMQConnectTask(factory, executor, mqttClient, "testQueue", 1, exitHandler);
        task.run();

        verify(executor).shutdown();
        verify(exitHandler).exit(1);
    }
}
