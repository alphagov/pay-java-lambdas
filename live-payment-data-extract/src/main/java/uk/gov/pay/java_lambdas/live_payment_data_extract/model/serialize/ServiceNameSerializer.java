package uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers.ServiceName;

import java.io.IOException;

public class ServiceNameSerializer extends JsonSerializer<ServiceName> {

    @Override
    public void serialize(ServiceName serviceName, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeRawValue(serviceName.en());
    }
}
