package uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators;

import javax.validation.constraints.Pattern;

public class CsvLastRow extends CsvRow {
    @Pattern(regexp = "^99$")
    private String headerRecord;
    
    public CsvLastRow() {}
}
