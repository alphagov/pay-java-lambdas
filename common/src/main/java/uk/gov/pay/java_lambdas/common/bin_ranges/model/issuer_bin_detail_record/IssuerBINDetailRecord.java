package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.AcceptsGamingOCTPayments;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.AnonymousPrepaidCardMarker;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.CardClass;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.DCCFlag;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.FastFundsIndicator;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.ProductType;
import uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums.SchemeProduct;

import java.io.Serializable;

public class IssuerBINDetailRecord implements Serializable {

    @JsonProperty(index = 0)
    @Pattern(regexp = "^01$", message = "record type must be 01")
    @Size(max = 2)
    @NotNull
    @NotBlank
    private String recordType;
    @JsonProperty(index = 1)
    @Size(max = 50)
    @NotNull
    @NotBlank
    private String issuerBinLowerRange;
    @JsonProperty(index = 2)
    @Size(max = 50)
    @NotNull
    @NotBlank
    private String issuerBinUpperRange;
    @JsonProperty(index = 3)
    @NotNull
    private ProductType productType;
    @JsonProperty(index = 4)
    @Size(max = 100)
    @NotNull
    @NotBlank
    private String cardSchemeBrandName;
    @JsonProperty(index = 5)
    @Size(max = 100)
    @NotNull
    @NotBlank
    private String issuerName;
    @JsonProperty(index = 6)
    @Size(max = 5)
    @NotNull
    @NotBlank
    private String issuerCountryCodeAlpha;
    @JsonProperty(index = 7)
    @Size(max = 5)
    @NotNull
    @NotBlank
    private String issuerCountryCodeNumeric;
    @JsonProperty(index = 8)
    @Size(max = 100)
    @NotNull
    @NotBlank
    private String issuerCountryName;
    @JsonProperty(index = 9)
    @NotNull
    private CardClass cardClass;
    @JsonProperty(index = 10)
    @Size(max = 5)
    private String cardholderCurrencyCode;
    @JsonProperty(index = 11)
    private DCCFlag dccFlag;
    @JsonProperty(index = 12)
    @NotNull
    private SchemeProduct schemeProduct;
    @JsonProperty(index = 13)
    @NotNull
    private AnonymousPrepaidCardMarker anonymousPrepaidCardMarker;
    @JsonProperty(index = 14)
    private AcceptsGamingOCTPayments acceptsGamingOCTPayments;
    @JsonProperty(index = 15)
    @Pattern(regexp = "^Y?$", message = "can only contain Y or be blank")
    private String tokenizedFLag;
    @JsonProperty(index = 16)
    @PositiveOrZero
    @Digits(integer = 2, fraction = 0)
    @NotNull
    private Integer panLength;
    @JsonProperty(index = 17)
    private FastFundsIndicator fastFundsIndicator;
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

    public IssuerBINDetailRecord() {
        // bean constructor
    }

    public String getRecordType() {
        return recordType;
    }

    public String getIssuerBinLowerRange() {
        return issuerBinLowerRange;
    }

    public String getIssuerBinUpperRange() {
        return issuerBinUpperRange;
    }

    public ProductType getProductType() {
        return productType;
    }

    public String getCardSchemeBrandName() {
        return cardSchemeBrandName;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public String getIssuerCountryCodeAlpha() {
        return issuerCountryCodeAlpha;
    }

    public String getIssuerCountryCodeNumeric() {
        return issuerCountryCodeNumeric;
    }

    public String getIssuerCountryName() {
        return issuerCountryName;
    }

    public CardClass getCardClass() {
        return cardClass;
    }

    public String getCardholderCurrencyCode() {
        return cardholderCurrencyCode;
    }

    public DCCFlag getDccFlag() {
        return dccFlag;
    }

    public SchemeProduct getSchemeProduct() {
        return schemeProduct;
    }

    public AnonymousPrepaidCardMarker getAnonymousPrepaidCardMarker() {
        return anonymousPrepaidCardMarker;
    }

    public AcceptsGamingOCTPayments getAcceptsGamingOCTPayments() {
        return acceptsGamingOCTPayments;
    }

    public String getTokenizedFLag() {
        return tokenizedFLag;
    }

    public Integer getPanLength() {
        return panLength;
    }

    public FastFundsIndicator getFastFundsIndicator() {
        return fastFundsIndicator;
    }

    public String getReserved19() {
        return reserved19;
    }

    public String getReserved20() {
        return reserved20;
    }

    public String getReserved21() {
        return reserved21;
    }

    public String getReserved22() {
        return reserved22;
    }

    public String getReserved23() {
        return reserved23;
    }

    public String getReserved24() {
        return reserved24;
    }

    public String getReserved25() {
        return reserved25;
    }
}

