package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum AnonymousPrepaidCardMarker {
    A("Anonymous prepaid program and not AMLD5 compliant"),
    E("Anonymous prepaid program and AMLD5 compliant"),
    N("Not prepaid or non-anonymous prepaid program/default"),
    U("Unknown");

    private final String description;

    AnonymousPrepaidCardMarker(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static AnonymousPrepaidCardMarker fromString(String key) {
        for (AnonymousPrepaidCardMarker anonymousPrepaidCardMarker : AnonymousPrepaidCardMarker.values()) {
            if (anonymousPrepaidCardMarker.name().equals(key)) {
                return anonymousPrepaidCardMarker;
            }
        }
        throw new IllegalArgumentException("Invalid AnonymousPrepaidCardMarker: " + key);
    }
}
