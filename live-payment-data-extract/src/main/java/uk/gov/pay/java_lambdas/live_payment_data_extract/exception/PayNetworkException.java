package uk.gov.pay.java_lambdas.live_payment_data_extract.exception;

public class PayNetworkException extends RuntimeException{

    public PayNetworkException(String message) {
        super(message);
    }
}
