package uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize.ServiceNameSerializer;

@JsonSerialize(using = ServiceNameSerializer.class)
public record ServiceName(String en, String cy) {
}
