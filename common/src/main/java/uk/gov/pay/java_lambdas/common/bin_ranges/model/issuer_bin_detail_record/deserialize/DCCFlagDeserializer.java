package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.deserialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.DCCFlag;

import java.io.IOException;

public class DCCFlagDeserializer extends JsonDeserializer<DCCFlag> {
    @Override
    public DCCFlag deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        String key = jsonParser.getText();
        try {
            return DCCFlag.fromString(key);
        } catch (IllegalArgumentException e) {
            throw new IOException("Error deserializing DCCFlag", e);
        }
    }
}
