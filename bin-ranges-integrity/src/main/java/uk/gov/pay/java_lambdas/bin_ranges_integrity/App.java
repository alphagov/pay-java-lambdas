package uk.gov.pay.java_lambdas.bin_ranges_integrity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String... args)
    {
        logger.info("Lambda starts");

        Handler handler = new Handler();
        handler.sendRequest();

        logger.info("Lambda ends");
    }
}
