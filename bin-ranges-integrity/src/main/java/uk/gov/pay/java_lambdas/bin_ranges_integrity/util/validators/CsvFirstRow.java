package uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators;

import javax.validation.constraints.Pattern;

public class CsvFirstRow extends CsvRow {
    @Pattern(regexp = "^00$")
    private String headerRecord;
    
    public CsvFirstRow() {}
}
