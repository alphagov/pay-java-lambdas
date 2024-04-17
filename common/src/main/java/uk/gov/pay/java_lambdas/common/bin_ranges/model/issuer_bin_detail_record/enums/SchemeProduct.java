package uk.gov.pay.java_lambdas.common.bin_ranges.model.issuer_bin_detail_record.enums;

public enum SchemeProduct {
    AC000("MasterCard Cr Per"),
    ACMCW("MasterCard Signia"),
    ACMCY("MasterCard Dr Per"),
    ACMNW("MasterCard World"),
    AS000("All Star"),
    AX000("American Express"),
    BC000("Visa Credit"),
    DC000("Diners Club/Discover"),
    DE000("Visa Debit Personal"),
    DECOM("Visa Debit Com"),
    DM000("Dr MasterCard Per"),
    DMCOM("Dr MasterCard Com"),
    LS000("LaSer"),
    JC000("JCB"),
    KF000("Keyfuels"),
    OD000("Overdrive"),
    PE000("Visa Electron Dr"),
    PECRE("Visa Electron Cr"),
    PM000("Maestro Per"),
    PMCOM("Maestro Com"),
    SC000("Supercharge"),
    SE000("Sears"),
    VP002("MasterCard Comm"),
    VPMCB("MasterCard Business"),
    VPMCF("MasterCard Fleet"),
    VPMCO("MasterCard Corporate"),
    VPMCP("MasterCard Purchase"),
    VPMCX("MasterCard Dr Com"),
    VPVIB("Visa Business"),
    VPVID("Visa Commerce"),
    VPVIR("Visa Corporate"),
    VPVIS("Visa Purchasing");

    private final String description;

    SchemeProduct(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static SchemeProduct fromString(String key) {
        for (SchemeProduct schemeProduct : SchemeProduct.values()) {
            if (schemeProduct.name().equals(key)) {
                return schemeProduct;
            }
        }
        throw new IllegalArgumentException("Invalid SchemeProduct: " + key);
    }
}
