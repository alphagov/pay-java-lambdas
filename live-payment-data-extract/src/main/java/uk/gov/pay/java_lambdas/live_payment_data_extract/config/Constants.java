package uk.gov.pay.java_lambdas.live_payment_data_extract.config;

import software.amazon.awssdk.regions.Region;

import java.util.Optional;

public class Constants {
    
    private Constants() {}

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final boolean UPDATE_SERVICES = Optional.of(Boolean.parseBoolean(System.getenv("UPDATE_SERVICES"))).orElse(false);
    public static final String DATA_BUCKET = System.getenv("DATA_BUCKET");
    public static final String USER_APPROVED_FOR_CAPTURE = "USER_APPROVED_FOR_CAPTURE";
    public static final String USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL = "USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL";
    public static final String EVENT_TICKER_ENDPOINT = "/v1/event/ticker";
    public static final String SERVICES_ENDPOINT = "/v1/api/services/list";
    public static final String PERFORMANCE_REPORT_ENDPOINT = "/v1/report/performance-report-legacy";
    public static final String TIME_SERIES_REPORT_ENDPOINT = "/v1/report/transactions-by-hour";

    private static final String LEDGER_URL = System.getenv("LEDGER_URL");
    private static final String ADMINUSERS_URL = System.getenv("ADMINUSERS_URL");

    public static String getLedgerUrl () {
        return LEDGER_URL;
    }
    
    public static String getAdminUsersUrl () {
        return ADMINUSERS_URL;
    }
}
