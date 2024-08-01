package uk.gov.pay.java_lambdas.live_payment_data_extract.config;

import software.amazon.awssdk.regions.Region;

import java.util.Optional;

public class Constants {
    
    private Constants() {}

    public static final Region AWS_REGION = Region.of(System.getenv("AWS_REGION"));
    public static final boolean UPDATE_SERVICES = Optional.of(Boolean.parseBoolean(System.getenv("UPDATE_SERVICES"))).orElse(false);
    public static final String LEDGER_URL = System.getenv("LEDGER_URL");
    public static final String ADMINUSERS_URL = System.getenv("ADMINUSERS_URL");
    public static final String DATA_BUCKET = System.getenv("DATA_BUCKET");
    public static final String USER_APPROVED_FOR_CAPTURE = "USER_APPROVED_FOR_CAPTURE";
    public static final String USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL = "USER_APPROVED_FOR_CAPTURE_AWAITING_SERVICE_APPROVAL";
}
