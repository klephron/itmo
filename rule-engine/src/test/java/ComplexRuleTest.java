import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jeasy.rules.api.Facts;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RuleMongoPayload;
import ru.itmo.zavar.rules.ComplexRule;
import ru.itmo.zavar.task.MongoDBConnectTask;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ComplexRuleTest {

    @Test
    void testConditionTrue() {
        Facts facts = new Facts();
        for (int i = 0; i < 10; i++) {
            String[] factData = {"1", "25.0", "65.0", "1000"};
            facts.put(String.valueOf(i), factData);
        }
        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);
        ComplexRule rule = new ComplexRule(mockMongo, "db", "col", "replica1");

        assertTrue(rule.when(facts));
    }

    @Test
    void testConditionFalse() {
        Facts facts = new Facts();
        for (int i = 0; i < 9; i++) {
            facts.put(String.valueOf(i), new String[]{"1", "25.0", "65.0", "1000"});
        }
        facts.put("9", new String[]{"1","25.0","55.0","1000"});

        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);
        ComplexRule rule = new ComplexRule(mockMongo, "db", "col", "replica1");

        assertFalse(rule.when(facts));
    }

    @Test
    void testThenCallsSendToMongo() throws InterruptedException {
        Facts facts = new Facts();
        for (int i = 0; i < 10; i++) {
            facts.put(String.valueOf(i), new String[]{"1", "25.0", "65.0", "1000"});
        }

        MongoDBConnectTask mockMongo = mock(MongoDBConnectTask.class);
        MongoClient mockClient = mock(MongoClient.class);
        MongoDatabase mockAdminDb = mock(MongoDatabase.class);
        MongoDatabase mockMainDb = mock(MongoDatabase.class);
        MongoCollection<RuleMongoPayload> mockCollection = mock(MongoCollection.class);

        when(mockClient.getDatabase("admin")).thenReturn(mockAdminDb);
        when(mockAdminDb.runCommand(any(Document.class)))
                .thenReturn(new Document("ok", 1));

        when(mockClient.getDatabase("db")).thenReturn(mockMainDb);
        when(mockMainDb.getCollection("col", RuleMongoPayload.class))
                .thenReturn(mockCollection);

        when(mockMongo.getMongoClient()).thenReturn(mockClient);

        ComplexRule rule = new ComplexRule(mockMongo, "db", "col", "replica1");

        assertTrue(rule.when(facts));
        rule.then(facts);

        verify(mockCollection).insertOne(any(RuleMongoPayload.class));
    }


}
