package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum CardClass {
    C("Credit"),
    D("Debit"),
    H("Charge Card (V4 only)"),
    P("Prepaid"),
    R("Deferred Debit (V4 only)");

    private final String description;

    CardClass(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }

    public static CardClass fromString(String key) {
        for (CardClass cardClass : CardClass.values()) {
            if (cardClass.name().equals(key)) {
                return cardClass;
            }
        }
        throw new IllegalArgumentException("Invalid CardClass: " + key);
    }
}
