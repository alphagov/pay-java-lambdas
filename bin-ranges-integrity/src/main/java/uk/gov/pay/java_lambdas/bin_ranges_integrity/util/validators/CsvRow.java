package uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators;

import software.amazon.awssdk.annotations.NotNull;

import javax.validation.constraints.Pattern;

public abstract class CsvRow {
    @Pattern(regexp = "^00|01|99$")
    private String headerRecord;
}
