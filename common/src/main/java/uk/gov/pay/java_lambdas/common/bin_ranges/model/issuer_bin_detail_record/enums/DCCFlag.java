package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize.DCCFlagDeserializer;

@JsonDeserialize(using = DCCFlagDeserializer.class)
public enum DCCFlag {
    DCC_ALLOWED("DCC allowed");

    private final String description;

    DCCFlag(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static DCCFlag fromString(String key) {
        if (key.isEmpty()) {
            return null;
        }
        for (DCCFlag dccFlag : DCCFlag.values()) {
            if (dccFlag.description.equalsIgnoreCase(key)) {
                return dccFlag;
            }
        }
        throw new IllegalArgumentException("Invalid DCCFlag: " + key);
    }
}
