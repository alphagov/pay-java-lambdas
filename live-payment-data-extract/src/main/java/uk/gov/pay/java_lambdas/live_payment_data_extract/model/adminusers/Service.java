package uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
    "externalId",
    "serviceName",
    "merchantDetails",
    "wentLiveDate"
})
public record Service(@JsonRawValue String externalId,
                      @JsonProperty(access = WRITE_ONLY) String[] gatewayAccountIds,
                      @JsonSerialize(using = ZonedDateTimeSerializer.class) ZonedDateTime wentLiveDate,
                      ServiceName serviceName, MerchantDetails merchantDetails) {

    @Override
    public String toString() {
        return "Service[" +
            "externalId=" + externalId +
            ", gatewayAccountIds=" + Arrays.toString(gatewayAccountIds) +
            ", wentLiveDate=" + wentLiveDate +
            ", serviceName=" + serviceName +
            ", merchantDetails=" + merchantDetails +
            ']';
    }
}
