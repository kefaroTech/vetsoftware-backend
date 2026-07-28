package com.vetsoftware.app.infrastructure.audit.outbox;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import software.amazon.awssdk.services.firehose.model.Record;

@Component
@ConditionalOnProperty(
        prefix = "vetsoftware.audit.outbox",
        name = "publisher-enabled",
        havingValue = "true")
final class AuditOutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(AuditOutboxPublisherJob.class);

    private final AuditOutboxRepository repository;
    private final AuditOutboxProperties properties;
    private final FirehoseClient firehose;
    private final AuditOutboxMetrics metrics;
    private final ScheduledJobTelemetry telemetry;

    AuditOutboxPublisherJob(
            AuditOutboxRepository repository,
            AuditOutboxProperties properties,
            FirehoseClient firehose,
            AuditOutboxMetrics metrics,
            ScheduledJobTelemetry telemetry) {
        properties.validate();
        this.repository = repository;
        this.properties = properties;
        this.firehose = firehose;
        this.metrics = metrics;
        this.telemetry = telemetry;
    }

    @Scheduled(
            fixedDelayString = "${vetsoftware.audit.outbox.publish-interval:PT5S}",
            initialDelayString = "${vetsoftware.audit.outbox.publish-initial-delay:PT10S}")
    void publish() {
        telemetry.observe("audit.outbox.publish", this::publishBatch);
    }

    ScheduledJobTelemetry.Outcome publishBatch() {
        Instant now = Instant.now();
        List<AuditOutboxRecord> batch =
                repository.claim(properties.getBatchSize(), now, properties.getLeaseDuration());
        if (batch.isEmpty()) {
            return ScheduledJobTelemetry.Outcome.NO_WORK;
        }

        try {
            PutRecordBatchResponse response = firehose.putRecordBatch(PutRecordBatchRequest.builder()
                    .deliveryStreamName(properties.getDeliveryStreamName())
                    .records(batch.stream().map(AuditOutboxPublisherJob::toFirehoseRecord).toList())
                    .build());
            return applyResponse(batch, response, now);
        } catch (RuntimeException exception) {
            batch.forEach(record -> fail(record, exception.getClass().getSimpleName() + ": "
                    + exception.getMessage(), now));
            metrics.failed(batch.size());
            log.error("Firehose no aceptó el lote de auditoría; eventos={}", batch.size(), exception);
            return ScheduledJobTelemetry.Outcome.FAILURE;
        }
    }

    private ScheduledJobTelemetry.Outcome applyResponse(
            List<AuditOutboxRecord> batch, PutRecordBatchResponse response, Instant now) {
        List<PutRecordBatchResponseEntry> entries = response.requestResponses();
        List<Long> publishedIds = new ArrayList<>(batch.size());
        int failures = 0;

        for (int index = 0; index < batch.size(); index++) {
            AuditOutboxRecord record = batch.get(index);
            PutRecordBatchResponseEntry entry = index < entries.size() ? entries.get(index) : null;
            if (entry != null && (entry.errorCode() == null || entry.errorCode().isBlank())) {
                publishedIds.add(record.id());
            } else {
                failures++;
                String error = entry == null
                        ? "missing_firehose_response"
                        : entry.errorCode() + ": " + entry.errorMessage();
                fail(record, error, now);
            }
        }

        repository.markPublished(publishedIds, now);
        metrics.published(publishedIds.size());
        metrics.failed(failures);
        if (failures > 0) {
            log.warn("Firehose aceptó parcialmente auditoría; publicados={} fallidos={}",
                    publishedIds.size(), failures);
        }
        return ScheduledJobTelemetry.Outcome.from(batch.size(), failures);
    }

    private void fail(AuditOutboxRecord record, String error, Instant now) {
        repository.markFailed(record.id(), now.plus(retryDelay(record.attempts())), error);
    }

    Duration retryDelay(int attempts) {
        int exponent = Math.min(Math.max(attempts - 1, 0), 30);
        long baseMillis = properties.getBaseRetryDelay().toMillis();
        long maxMillis = properties.getMaxRetryDelay().toMillis();
        long exponential;
        try {
            exponential = Math.multiplyExact(baseMillis, 1L << exponent);
        } catch (ArithmeticException exception) {
            exponential = maxMillis;
        }
        long capped = Math.min(exponential, maxMillis);
        double jitter = ThreadLocalRandom.current().nextDouble(0.75, 1.25);
        return Duration.ofMillis(Math.max(1, Math.min((long) (capped * jitter), maxMillis)));
    }

    private static Record toFirehoseRecord(AuditOutboxRecord record) {
        byte[] newlineDelimitedJson =
                (record.payload() + "\n").getBytes(StandardCharsets.UTF_8);
        return Record.builder()
                .data(SdkBytes.fromByteBuffer(ByteBuffer.wrap(newlineDelimitedJson)))
                .build();
    }
}
