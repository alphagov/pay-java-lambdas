package uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize.MerchantDetailsSerializer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = MerchantDetailsSerializer.class)
public record MerchantDetails(String name) {
}
