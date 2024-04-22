package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum AcceptsGamingOCTPayments {
    Y("Does accept gaming OCT payments"),
    N("Does not accept gaming OCT payments");

    private final String description;

    AcceptsGamingOCTPayments(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static AcceptsGamingOCTPayments fromString(String key) {
        for (AcceptsGamingOCTPayments acceptsGamingOCTPayments : AcceptsGamingOCTPayments.values()) {
            if (acceptsGamingOCTPayments.name().equals(key)) {
                return acceptsGamingOCTPayments;
            }
        }
        throw new IllegalArgumentException("Invalid AcceptsGamingOCTPayments: " + key);
    }
}
