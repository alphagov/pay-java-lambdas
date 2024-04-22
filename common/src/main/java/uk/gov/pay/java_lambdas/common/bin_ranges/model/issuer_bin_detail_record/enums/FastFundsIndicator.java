package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum FastFundsIndicator {
    Y("Domestic and Cross-Border Fast Funds supported"),
    C("Cross-Border Fast Funds supported only"),
    D("Domestic Fast Funds supported only"),
    N("Does not participate in Fast Funds");

    private final String description;

    FastFundsIndicator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static FastFundsIndicator fromString(String key) {
        for (FastFundsIndicator fastFundsIndicator : FastFundsIndicator.values()) {
            if (fastFundsIndicator.name().equals(key)) {
                return fastFundsIndicator;
            }
        }
        throw new IllegalArgumentException("Invalid FastFundsIndicator: " + key);
    }
}
