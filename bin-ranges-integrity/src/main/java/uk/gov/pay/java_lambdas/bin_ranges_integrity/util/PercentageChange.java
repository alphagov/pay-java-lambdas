package uk.gov.pay.java_lambdas.bin_ranges_integrity.util;

public class PercentageChange {

    private PercentageChange() {
    }
    public static double get(long candidate, long promoted) {
        if (promoted == 0) throw new IllegalArgumentException("Promoted value cannot be 0");
        return ((double) (candidate - promoted) / promoted) * 100;
    }
}
