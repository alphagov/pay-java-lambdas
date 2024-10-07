package uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.serialize.ZonedDateTimeSerializer;

import java.time.ZonedDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TimeSeriesReportSlice(@JsonSerialize(using = ZonedDateTimeSerializer.class) ZonedDateTime timestamp,
                                    int allPayments,
                                    int erroredPayments,
                                    int completedPayments,
                                    int amount,
                                    int netAmount,
                                    int totalAmount,
                                    int fee) {
}
