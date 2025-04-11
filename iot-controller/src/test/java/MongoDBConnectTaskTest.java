import com.mongodb.MongoClientSettings;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.exithandler.DefaultExitHandler;
import ru.itmo.zavar.task.exithandler.ExitHandler;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MongoDBConnectTaskTest {

    @Test
    void testSuccessfulConnection() {
        ExecutorService executor = mock(ExecutorService.class);
        MongoClientSettings settings = MongoClientSettings.builder().build();

        MongoDBConnectTask task = new MongoDBConnectTask(settings, executor, 1, new DefaultExitHandler());
        task.run();

        assertNotNull(task.getMongoClient());
        verify(executor).shutdown();
    }

    @Test
    void testFailedConnection() {
        ExecutorService executor = mock(ExecutorService.class);
        ExitHandler exitHandler = mock(ExitHandler.class);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new com.mongodb.ConnectionString("mongodb://invalid:27017"))
                .build();

        MongoDBConnectTask task = new MongoDBConnectTask(settings, executor, 1, exitHandler);
        task.run();

        assertNull(task.getMongoClient());
        verify(executor).shutdown();
        verify(exitHandler).exit(1);
    }
}
