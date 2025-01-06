package uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers.MerchantDetails;

import java.io.IOException;

public class MerchantDetailsSerializer extends JsonSerializer<MerchantDetails> {

    @Override
    public void serialize(MerchantDetails merchantDetails, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeRawValue(merchantDetails.name());
    }
}
