package uk.gov.pay.java_lambdas.common.bin_ranges.dto;


import java.io.Serializable;
import java.time.Instant;

public record Candidate(String s3Key, boolean proceed, Instant time, String failureMessage) implements Serializable {

    public Candidate(String s3Key, boolean proceed) {
        this(s3Key, proceed, Instant.now(), null);
    }

    public Candidate(String s3Key, boolean proceed, String failureMessage) {
        this(s3Key, proceed, Instant.now(), failureMessage);
    }

    public static Candidate proceed(Candidate existingCandidate) {
        return new Candidate(existingCandidate.s3Key(), true, existingCandidate.time(), existingCandidate.failureMessage());
    }

    public static Candidate halt(Candidate existingCandidate, String failureMessage) {
        return new Candidate(existingCandidate.s3Key(), false, existingCandidate.time(), failureMessage);
    }
}
