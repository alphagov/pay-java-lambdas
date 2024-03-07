package uk.gov.pay.java_lambdas.common.bin_ranges.dto;

import java.io.Serializable;
import java.time.Instant;

public record Candidate(String s3Key, boolean proceed, Instant time) implements Serializable {
    
    public Candidate (String s3Key, boolean proceed) {  
        this(s3Key, proceed, Instant.now());
    }

    public static Candidate from(Candidate existingCandidate, boolean proceed) {
        return new Candidate(existingCandidate.s3Key(), proceed, existingCandidate.time());
    }
}
