import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jeasy.rules.api.Facts;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RuleMongoPayload;
import ru.itmo.zavar.rules.SimpleRule;
import ru.itmo.zavar.task.MongoDBConnectTask;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SimpleRuleTest {

    @Test
    void testConditionTrue() {
        String[] factData = {"1","25.0","75.0","123"};
        Facts facts = new Facts();
        facts.put("0", factData);

        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);
        SimpleRule rule = new SimpleRule(mockMongo, "db", "col", "replica1");

        boolean result = rule.when(facts);
        assertTrue(result, "Should be true when radiatorTemp>70");
    }

    @Test
    void testConditionFalse() {
        String[] factData = {"1","25.0","65.0","123"};
        Facts facts = new Facts();
        facts.put("0", factData);

        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);
        SimpleRule rule = new SimpleRule(mockMongo, "db", "col", "replica1");

        boolean result = rule.when(facts);
        assertFalse(result, "Should be false when radiatorTemp<=70");
    }

    @Test
    void testThenCallsSendToMongo() throws InterruptedException {
        String[] factData = {"1","25.0","75.0","123"};
        Facts facts = new Facts();
        facts.put("0", factData);

        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);

        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockAdminDb = mock(MongoDatabase.class);
        MongoDatabase mockMainDb = mock(MongoDatabase.class);
        MongoCollection<RuleMongoPayload> mockCollection = mock(MongoCollection.class);

        when(mockClient.getDatabase("admin")).thenReturn(mockAdminDb);
        when(mockAdminDb.runCommand(any())).thenReturn(new Document("ok", 1));

        when(mockClient.getDatabase("db")).thenReturn(mockMainDb);

        when(mockMainDb.getCollection("col", RuleMongoPayload.class))
                .thenReturn(mockCollection);

        when(mockMongo.getMongoClient()).thenReturn(mockClient);

        SimpleRule rule = new SimpleRule(mockMongo, "db", "col", "replica1");

        assertTrue(rule.when(facts));

        rule.then(facts);

        verify(mockCollection).insertOne(any(RuleMongoPayload.class));
    }


}
