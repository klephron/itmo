import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RuleMongoPayload;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
public class RuleMongoPayloadTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testValidRuleMongoPayload() {
        RuleMongoPayload payload =
                new RuleMongoPayload("ruleName", "replica1", System.currentTimeMillis(), 123L, "{}");
        Set<ConstraintViolation<RuleMongoPayload>> violations = validator.validate(payload);
        assertTrue(violations.isEmpty(), "Should be valid");
    }

    @Test
    void testEmptyRuleName() {
        RuleMongoPayload payload =
                new RuleMongoPayload("", "replica1", System.currentTimeMillis(), 123L, "{}");
        Set<ConstraintViolation<RuleMongoPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Should fail when ruleName is empty");
    }

    @Test
    void testNegativeRadiatorId() {
        RuleMongoPayload payload =
                new RuleMongoPayload("ruleName", "replica", System.currentTimeMillis(), -1L, "{}");
        Set<ConstraintViolation<RuleMongoPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Should fail when radiatorId < 1");
    }
}
