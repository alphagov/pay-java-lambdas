package uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
    "totalVolume",
    "totalAmount",
    "averageAmount"
})
public record PerformanceReport (int totalVolume, int totalAmount, double averageAmount) {
}
