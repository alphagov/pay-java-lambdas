package uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
    "resourceExternalId",
    "eventDate",
    "eventType",
    "serviceExternalId",
    "cardBrand",
    "amount",
    "walletType",
    "source",
    "isMoto",
    "isRecurring"
})
public record EventTicker(@JsonRawValue String resourceExternalId, 
                          @JsonSerialize(using = ZonedDateTimeSerializer.class) ZonedDateTime eventDate,
                          @JsonRawValue String eventType, 
                          @JsonRawValue String serviceExternalId,
                          @JsonRawValue String cardBrand,
                          @JsonRawValue Long amount,
                          @JsonRawValue String walletType,
                          @JsonRawValue String source,
                          @JsonRawValue String isMoto,
                          @JsonRawValue String isRecurring) {
}
