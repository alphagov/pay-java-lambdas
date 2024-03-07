package uk.gov.pay.java_lambdas.common.bin_ranges.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidateTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    @Test
    void from() {
        var expected = new Candidate("an/s3/key.csv", true, Instant.now());
        var result = Candidate.from(expected, false);
        
        assertEquals(expected.s3Key(), result.s3Key());
        assertEquals(expected.time(), result.time());
        assertTrue(expected.proceed());
        assertFalse(result.proceed());
    }

    @Test
    void candidate_ShouldSerialize_andDeserialize() throws JsonProcessingException {
        var expected = new Candidate("an/s3/key.csv", true, Instant.now());
        var jsonStr = objectMapper.writeValueAsString(expected);
        var result = objectMapper.readValue(jsonStr, Candidate.class);

        assertEquals(expected.s3Key(), result.s3Key());
        assertEquals(expected.proceed(), result.proceed());
        assertEquals(expected.time(), result.time());
    }
}
