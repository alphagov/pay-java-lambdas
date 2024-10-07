package uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

import static java.lang.String.join;

public class StringArraySerializer extends JsonSerializer<String[]> {
    
    @Override
    public void serialize(String[] strings, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(join("|", strings));
    }
}
