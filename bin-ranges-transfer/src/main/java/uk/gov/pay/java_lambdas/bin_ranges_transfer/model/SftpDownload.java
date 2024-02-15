package uk.gov.pay.java_lambdas.bin_ranges_transfer.model;

public record SftpDownload(String filePath, String fileName, String date, Long size) {
    
    public String getS3Key() {
        return String.format("%s/%s", date, fileName);
    }
}
