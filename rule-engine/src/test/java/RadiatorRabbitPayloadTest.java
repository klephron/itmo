import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RadiatorRabbitPayloadTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testValidPayload() {
        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(1L, 60.0, 22.0, System.currentTimeMillis(), 0L);
        Set<ConstraintViolation<RadiatorRabbitPayload>> violations = validator.validate(payload);
        assertTrue(violations.isEmpty(), "No validation errors expected for valid payload");
    }

    @Test
    void testInvalidRadiatorTemp() {
        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(1L, 30.0, 22.0, System.currentTimeMillis(), 0L);
        Set<ConstraintViolation<RadiatorRabbitPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Should fail when radiatorTemp < 55");
    }

    @Test
    void testInvalidRoomTemp() {
        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(1L, 65.0, 50.0, System.currentTimeMillis(), 0L);
        Set<ConstraintViolation<RadiatorRabbitPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Should fail when roomTemp > 43");
    }

    @Test
    void testDeviceIdNull() {
        RadiatorRabbitPayload payload =
                new RadiatorRabbitPayload(null, 60.0, 22.0, System.currentTimeMillis(), 0L);
        Set<ConstraintViolation<RadiatorRabbitPayload>> violations = validator.validate(payload);
        assertFalse(violations.isEmpty(), "Should fail when deviceId is null");
    }
}
