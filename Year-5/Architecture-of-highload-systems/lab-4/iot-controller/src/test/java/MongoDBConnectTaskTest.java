package ru.itmo.zavar.task;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.task.exithandler.ExitHandler;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class MongoDBConnectTaskTest {

    private MongoClientSettings settings;
    private ExecutorService executor;
    private ExitHandler exitHandler;
    private MongoClientFactory factory;

    @BeforeEach
    void setUp() {
        settings = MongoClientSettings.builder().build();
        executor = mock(ExecutorService.class);
        exitHandler = mock(ExitHandler.class);
        factory = mock(MongoClientFactory.class);
    }

    @Test
    void testSuccessfulConnection() {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDb = mock(MongoDatabase.class);

        when(factory.create(settings)).thenReturn(mockClient);
        when(mockClient.getDatabase("admin")).thenReturn(mockDb);
        when(mockDb.runCommand(any(Document.class))).thenReturn(new Document("ok", 1));

        MongoDBConnectTask task = new MongoDBConnectTask(settings, executor, 2, exitHandler, factory);
        task.run();

        assertNotNull(task.getMongoClient(), "MongoClient должен быть создан");
        assertSame(mockClient, task.getMongoClient(), "mongoClient должен совпадать с mockClient");
        verify(executor).shutdown();
        verify(exitHandler, never()).exit(anyInt());
    }

    @Test
    void testFailedConnectionExhaustedRetries() {
        when(factory.create(settings)).thenThrow(new RuntimeException("test error"));
        MongoDBConnectTask task = new MongoDBConnectTask(settings, executor, 1, exitHandler, factory);

        task.run();

        assertNull(task.getMongoClient());
        verify(executor).shutdown();
        verify(exitHandler).exit(1);
    }

    @Test
    void testFailedConnectionCloseClientIfNotNull() {
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockDb = mock(MongoDatabase.class);

        when(factory.create(settings)).thenReturn(mockClient);
        when(mockClient.getDatabase("admin")).thenReturn(mockDb);
        when(mockDb.runCommand(any(Document.class))).thenThrow(new RuntimeException("Ping failed"));

        MongoDBConnectTask task = new MongoDBConnectTask(settings, executor, 1, exitHandler, factory);
        task.run();

        verify(mockClient).close();
        assertNull(task.getMongoClient());
        verify(executor).shutdown();
        verify(exitHandler).exit(1);
    }
}
