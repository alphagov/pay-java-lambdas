package uk.gov.pay.java_lambdas.common.bin_ranges.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

class CandidateTest {

    private ObjectMapper objectMapper;
    private final Instant fixedInstant = Instant.now();
    private MockedStatic<Instant> instantMockedStatic;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
        instantMockedStatic = mockStatic(Instant.class, CALLS_REAL_METHODS);
        instantMockedStatic.when(Instant::now).thenReturn(fixedInstant);
    }

    @AfterEach
    public void tearDown() {
        instantMockedStatic.close();
    }
    
    @Test
    void proceed() {
        Candidate candidate = new Candidate("an/s3/key.csv", true);
        Candidate result = Candidate.proceed(candidate);

        assertEquals(fixedInstant, candidate.time());
        assertEquals(candidate.s3Key(), result.s3Key());
        assertEquals(candidate.time(), result.time());
        assertNull(result.failureMessage());
        assertTrue(result.proceed());
    }

    @Test
    void halt() {
        String failureMessage = "I failed for some reason";
        Candidate candidate = new Candidate("an/s3/key.csv", true);
        Candidate result = Candidate.halt(candidate, failureMessage);

        assertEquals(fixedInstant, candidate.time());
        assertEquals(candidate.s3Key(), result.s3Key());
        assertEquals(candidate.time(), result.time());
        assertEquals(failureMessage, result.failureMessage());
        assertFalse(result.proceed());
    }

    @Test
    void candidate_ShouldSerialize_andDeserialize() throws JsonProcessingException {
        Candidate expected = new Candidate("an/s3/key.csv", false, "A failure message");
        String jsonStr = objectMapper.writeValueAsString(expected);
        Candidate result = objectMapper.readValue(jsonStr, Candidate.class);

        assertEquals(fixedInstant, expected.time());
        assertEquals(expected.s3Key(), result.s3Key());
        assertEquals(expected.proceed(), result.proceed());
        assertEquals(expected.time(), result.time());
    }
}
