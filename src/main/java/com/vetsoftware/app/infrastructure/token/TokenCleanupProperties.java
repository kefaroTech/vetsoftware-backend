package com.vetsoftware.app.infrastructure.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vetsoftware.token-cleanup")
public class TokenCleanupProperties {

    private boolean enabled = true;
    private Duration retention = Duration.ofDays(7);
    private int batchSize = 1_000;
    private int maxBatchesPerRun = 10;
    private long growthWarningThreshold = 50_000;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getRetention() { return retention; }
    public void setRetention(Duration retention) { this.retention = retention; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxBatchesPerRun() { return maxBatchesPerRun; }
    public void setMaxBatchesPerRun(int maxBatchesPerRun) { this.maxBatchesPerRun = maxBatchesPerRun; }
    public long getGrowthWarningThreshold() { return growthWarningThreshold; }
    public void setGrowthWarningThreshold(long growthWarningThreshold) {
        this.growthWarningThreshold = growthWarningThreshold;
    }

    void validate() {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalStateException("vetsoftware.token-cleanup.retention debe ser positiva");
        }
        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalStateException(
                    "vetsoftware.token-cleanup.batch-size debe estar entre 1 y 10000");
        }
        if (maxBatchesPerRun < 1 || maxBatchesPerRun > 100) {
            throw new IllegalStateException(
                    "vetsoftware.token-cleanup.max-batches-per-run debe estar entre 1 y 100");
        }
        if (growthWarningThreshold < 1) {
            throw new IllegalStateException(
                    "vetsoftware.token-cleanup.growth-warning-threshold debe ser positivo");
        }
    }
}
