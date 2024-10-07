package uk.gov.pay.java_lambdas.live_payment_data_extract;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.utils.Pair;
import uk.gov.pay.java_lambdas.live_payment_data_extract.exception.PayNetworkException;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.adminusers.Service;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder.EventTicker;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder.PerformanceReport;
import uk.gov.pay.java_lambdas.live_payment_data_extract.model.legder.TimeSeriesReportSlice;
import uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DynamoDB;
import uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Http;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.DATA_BUCKET;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.EVENT_TICKER_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.PERFORMANCE_REPORT_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.SERVICES_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.TIME_SERIES_REPORT_ENDPOINT;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.UPDATE_SERVICES;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.USER_APPROVED_FOR_CAPTURE;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.getAdminUsersUrl;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.config.Constants.getLedgerUrl;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.createIntervals;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.DateTime.getDateString;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.getLastEventTSFromS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.getStringFromS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.S3.putStringToS3;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.mungeStringArray;
import static uk.gov.pay.java_lambdas.live_payment_data_extract.utils.Strings.trimTrailingNewLine;

public class App implements RequestHandler<Object, Object> {
    private final SdkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final CsvMapper csvMapper;
    private final CsvSchema eventTickerCsvSchema;
    private final CsvSchema serviceCsvSchema;
    private Instant now;
    
    private static final String TODAY = getDateString(Instant.now());
    private static final String EVENTS_CSV_FILE_NAME = format("%s-events.csv", TODAY);
    private static final String SERVICES_CSV_FILE_NAME = format("%s-services.csv", TODAY);
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public App() {
        httpClient = DependencyFactory.httpClient();
        objectMapper = DependencyFactory.objectMapper();
        csvMapper = DependencyFactory.csvMapper();
        eventTickerCsvSchema = csvMapper.schemaFor(EventTicker.class).withoutHeader();
        serviceCsvSchema = csvMapper.schemaFor(Service.class).withoutHeader();
    }
    
    /*
    scheduled in 5 min intervals
    every 24 hours, update services
     */

    @Override
    public Object handleRequest(final Object input, final Context context) {
        now = Instant.now();
        logger.info("fn: {}, version: {}.", context.getFunctionName(), context.getFunctionVersion());
        logger.info("invocation time: {}", now);
        DynamoDB.get(); // TODO remove me
        try {
            if (UPDATE_SERVICES) {
                logger.info("Updating services");
                retrieveServices();
            }
            retrieveTransactionTotals();
            retrieveTimeSeriesData();
            retrieveEvents();
        } catch (IOException | ExecutionException e) {
            throw new PayNetworkException(format("Problem extracting data: %s", e.getMessage()));
        } catch (InterruptedException e) {
            logger.error("Interrupted while extracting data: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        return input;
    }

    public void retrieveEvents() throws IOException, ExecutionException, InterruptedException {
        List<String> eventsCsvData = new ArrayList<>();
        Optional<String> serialisedEventsLog = getStringFromS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME, "\n");
        AtomicBoolean useTimeFromFile = new AtomicBoolean(false);

        /* 
        if previous events are available on S3, we use the last event timestamp recorded as our starting point when
        generating intervals for querying the ledger event ticker
        */
        serialisedEventsLog.ifPresentOrElse(
            events -> {
                eventsCsvData.add(trimTrailingNewLine(events));
                logger.info("Events log loaded for {}", TODAY);
                useTimeFromFile.set(true);
            },
            () -> logger.info("No previous events found for {}, defaulting to 5 minutes ago", TODAY)
        );

        List<Pair<String, String>> intervals = createIntervals(
            useTimeFromFile.get() ?
                getLastEventTSFromS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME) :
                now.minus(5L, ChronoUnit.MINUTES),
            now,
            Duration.of(30, ChronoUnit.SECONDS)
        );

        intervals.forEach(i -> {
            try {
                HttpExecuteRequest request = Http.getRequest(
                    URI.create(getLedgerUrl() + EVENT_TICKER_ENDPOINT),
                    Map.of(
                        "from_date", i.left(),
                        "to_date", i.right(),
                        "event_types", new String[]{USER_APPROVED_FOR_CAPTURE, USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL}
                    )
                );

                HttpExecuteResponse res = httpClient.prepareRequest(request)
                    .call();

                List<EventTicker> events = Http.handleResponse(res, responseBodyString -> {
                    try {
                        return mapJsonArrayToList(responseBodyString, objectMapper, EventTicker.class);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error parsing Events", e);
                    }
                });

                eventsCsvData.add(serializeToCsv(events, eventTickerCsvSchema, csvMapper));
                
            } catch (IOException e) {
                throw new PayNetworkException(e.getMessage());
            }
        });
        // TODO send to ddb
        putStringToS3(DATA_BUCKET, EVENTS_CSV_FILE_NAME, mungeStringArray(eventsCsvData));
        logger.debug("Finished retrieving events");
    }

    public void retrieveServices() throws IOException {
        try {
            HttpExecuteRequest request = Http.getRequest(
                URI.create(getAdminUsersUrl() + SERVICES_ENDPOINT),
                Collections.emptyMap()
            );
            
            HttpExecuteResponse res = httpClient.prepareRequest(request)
                .call();
            
            List<Service> services = Http.handleResponse(res, responseBodyString -> {
                try {
                    return mapJsonArrayToList(responseBodyString, objectMapper, Service.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error parsing Services", e);
                }
            });

            // TODO send to ddb
            putStringToS3(DATA_BUCKET, SERVICES_CSV_FILE_NAME, serializeToCsv(services, serviceCsvSchema, csvMapper));
            
        } catch (IOException e) {
            throw new PayNetworkException(e.getMessage());
        }
        logger.debug("Finished retrieving services");
    }

    public void retrieveTransactionTotals() {
        try {
            HttpExecuteRequest request = Http.getRequest(
                URI.create(getLedgerUrl() + PERFORMANCE_REPORT_ENDPOINT),
                Map.of(
                    "from_date", now.truncatedTo(ChronoUnit.DAYS).toString(),
                    "to_date", now.toString(),
                    "state", "SUCCESS"
                )
            );

            HttpExecuteResponse res = httpClient.prepareRequest(request)
                .call();

            PerformanceReport report = Http.handleResponse(res, responseBodyString -> {
                try {
                    return objectMapper.readValue(responseBodyString, PerformanceReport.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Error parsing PerformanceReport", e);
                }
            });
            
            // TODO send to ddb
            logger.info("Report: {}", report);
            
        } catch (IOException e) {
            throw new PayNetworkException(e.getMessage());
        }
        logger.debug("Finished retrieving performance report");
    }

    public void retrieveTimeSeriesData() {
        try {
            HttpExecuteRequest request = Http.getRequest(
                URI.create(getLedgerUrl() + TIME_SERIES_REPORT_ENDPOINT),
                Map.of(
                    "from_date", now.truncatedTo(ChronoUnit.DAYS).toString(),
                    "to_date", now.toString()
                )
            );

            HttpExecuteResponse res = httpClient.prepareRequest(request)
                .call();
            
            List<TimeSeriesReportSlice> reportSlices = Http.handleResponse(res, bodyContentString -> {
               try {
                   return mapJsonArrayToList(bodyContentString, objectMapper, TimeSeriesReportSlice.class);
               } catch (JsonProcessingException e) {
                   throw new RuntimeException("Error parsing TimeSeriesReportSlice", e);
               }
            });

            // TODO send to ddb
            logger.info("Slices: {}", reportSlices);
            
        } catch (IOException e) {
            throw new PayNetworkException(e.getMessage());
        }
        logger.debug("Finished retrieving aggregate transaction totals");
    }


    private <T> List<T> mapJsonArrayToList(String jsonArray, ObjectMapper objectMapper, Class<T> clazz) throws JsonProcessingException {
        List<T> mappedObjects = objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        logger.info("Retrieved {} count: {}", clazz.getSimpleName(), mappedObjects.size());
        return mappedObjects;
    }

    private <T> String serializeToCsv(List<T> records, CsvSchema schema, CsvMapper csvMapper) throws IOException {
        ObjectWriter writer = csvMapper.writer(schema);
        String csvOutput = writer.writeValueAsString(records);
        return trimTrailingNewLine(csvOutput);
    }
}
