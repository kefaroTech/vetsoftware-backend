package com.vetsoftware.app.infrastructure.audit.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties("vetsoftware.audit.outbox")
public class AuditOutboxProperties {

    private boolean enabled;
    private boolean publisherEnabled;
    private String deliveryStreamName;
    private String region = "sa-east-1";
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private int batchSize = 100;
    private Duration leaseDuration = Duration.ofMinutes(2);
    private Duration baseRetryDelay = Duration.ofSeconds(5);
    private Duration maxRetryDelay = Duration.ofMinutes(15);
    private Duration retention = Duration.ofDays(7);
    private int cleanupBatchSize = 10_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isPublisherEnabled() { return publisherEnabled; }
    public void setPublisherEnabled(boolean publisherEnabled) { this.publisherEnabled = publisherEnabled; }
    public String getDeliveryStreamName() { return deliveryStreamName; }
    public void setDeliveryStreamName(String deliveryStreamName) { this.deliveryStreamName = deliveryStreamName; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public Duration getLeaseDuration() { return leaseDuration; }
    public void setLeaseDuration(Duration leaseDuration) { this.leaseDuration = leaseDuration; }
    public Duration getBaseRetryDelay() { return baseRetryDelay; }
    public void setBaseRetryDelay(Duration baseRetryDelay) { this.baseRetryDelay = baseRetryDelay; }
    public Duration getMaxRetryDelay() { return maxRetryDelay; }
    public void setMaxRetryDelay(Duration maxRetryDelay) { this.maxRetryDelay = maxRetryDelay; }
    public Duration getRetention() { return retention; }
    public void setRetention(Duration retention) { this.retention = retention; }
    public int getCleanupBatchSize() { return cleanupBatchSize; }
    public void setCleanupBatchSize(int cleanupBatchSize) { this.cleanupBatchSize = cleanupBatchSize; }

    void validate() {
        if (batchSize < 1 || batchSize > 500) {
            throw new IllegalStateException("vetsoftware.audit.outbox.batch-size debe estar entre 1 y 500");
        }
        if (cleanupBatchSize < 1) {
            throw new IllegalStateException("vetsoftware.audit.outbox.cleanup-batch-size debe ser positivo");
        }
        if (leaseDuration.isNegative() || leaseDuration.isZero()
                || baseRetryDelay.isNegative() || baseRetryDelay.isZero()
                || maxRetryDelay.compareTo(baseRetryDelay) < 0
                || retention.isNegative() || retention.isZero()) {
            throw new IllegalStateException("Duraciones inválidas en vetsoftware.audit.outbox");
        }
        if (publisherEnabled && (deliveryStreamName == null || deliveryStreamName.isBlank())) {
            throw new IllegalStateException(
                    "vetsoftware.audit.outbox.delivery-stream-name es obligatorio con publisher-enabled=true");
        }
        if (StringUtils.hasText(accessKey) != StringUtils.hasText(secretKey)) {
            throw new IllegalStateException(
                    "audit.outbox access-key y secret-key deben configurarse juntos");
        }
    }
}
