package uk.gov.pay.java_lambdas.bin_ranges_integrity.util.validators;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.*;

public class CsvMiddleRow extends CsvRow {
    @JsonProperty(index = 0)
    @Pattern(regexp = "^01$", message = "Column A must have the value 01")
    private String headerRecord;
    @JsonProperty(index = 1)
    @Digits(integer = 11, fraction = 0, message = "Lower BIN Range must be an 11 digit number")
    private long binLowerRange;
    @JsonProperty(index = 2)
    @Digits(integer = 11, fraction = 0, message = "Upper BIN Range must be an 11 digit number")
    private long binUpperRange;
    @JsonProperty(index = 3)
    @Pattern(regexp = "^CN|CP$", message = "Product type is expected to be CN or CP")
    private String productType;
    @JsonProperty(index = 4)
    @Size(min = 2, max = 30, message = "Card Scheme Brand Name should be between 2 and 30 chars long")
    private String cardSchemeBrandName;
    @JsonProperty(index = 5)
    @Size(max = 80, message = "Issuer Name should be between 2 and 30 chars long")
    private String issuerName;
    @JsonProperty(index = 6)
    @Size(min = 3, max = 3, message = "Cardholder Currency Code should be exactly 3 chars long")
    private String issuerCountryCode;
    @JsonProperty(index = 7)
    private String issuerCountryCodeNumeric;
    @JsonProperty(index = 8)
    private String issuerCountryName;
    @JsonProperty(index = 9)
    private String cardClass;
    @JsonProperty(index = 10)
    private String cardholderCurrencyCode;
    @JsonProperty(index = 11)
    @Pattern(regexp = "^[NEAU]?$", message = "Anonymous Pre-Paid Flag can only contain N, E, A, or U")
    private String dccFlag;
    @JsonProperty(index = 12)
    private String schemeProduct;
    @JsonProperty(index = 13)
    private String anonPrepaidMarker;
    @JsonProperty(index = 14)
    @Pattern(regexp = "^[YN]?$", message = "Gaming OCT Payments can only contain Y, N, or be blank")
    private String acceptsGamingOCTPayments;
    @JsonProperty(index = 15)
    @Pattern(regexp = "^Y?$", message = "Tokenised Flag can only contain Y or be blank")
    private String tokenisedFile;
    @JsonProperty(index = 16)
    @Size(max = 19, message = "Pan length is greater than 19")
    private String panLength;
    @JsonProperty(index = 17)
    @Pattern(regexp = "^[DNYC]?$", message = "Fast Funds Indicator can only contain D, N, Y, C, or be blank")
    private String fastFundsIndicator;
    @JsonProperty(index = 18)
    private String reserved19;
    @JsonProperty(index = 19)
    private String reserved20;
    @JsonProperty(index = 20)
    private String reserved21;
    @JsonProperty(index = 21)
    private String reserved22;
    @JsonProperty(index = 22)
    private String reserved23;
    @JsonProperty(index = 23)
    private String reserved24;
    @JsonProperty(index = 24)
    private String reserved25;

    public CsvMiddleRow() {
        // jackson no-arg deserialization constructor
    }
}
