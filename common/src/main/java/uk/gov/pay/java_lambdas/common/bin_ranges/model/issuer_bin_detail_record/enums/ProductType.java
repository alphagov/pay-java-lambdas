package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum ProductType {
    CP("Commercial or Corporate Card"),
    CN("Consumer Card");

    private final String description;

    ProductType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ProductType fromString(String key) {
        for (ProductType productType : ProductType.values()) {
            if (productType.name().equals(key)) {
                return productType;
            }
        }
        throw new IllegalArgumentException("Invalid ProductType: " + key);
    }
}
