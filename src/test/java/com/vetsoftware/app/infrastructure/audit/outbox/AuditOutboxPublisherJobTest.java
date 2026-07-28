package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;

class AuditOutboxPublisherJobTest {

    private final AuditOutboxRepository repository = mock(AuditOutboxRepository.class);
    private final FirehoseClient firehose = mock(FirehoseClient.class);
    private final AuditOutboxMetrics metrics = mock(AuditOutboxMetrics.class);
    private final ScheduledJobTelemetry telemetry = mock(ScheduledJobTelemetry.class);
    private final AuditOutboxProperties properties = new AuditOutboxProperties();
    private AuditOutboxPublisherJob job;

    @BeforeEach
    void setUp() {
        properties.setPublisherEnabled(true);
        properties.setDeliveryStreamName("audit-stream");
        properties.setBatchSize(100);
        job = new AuditOutboxPublisherJob(repository, properties, firehose, metrics, telemetry);
    }

    @Test
    void appliesPartialFirehoseResponsePerRecord() {
        List<AuditOutboxRecord> records = List.of(
                new AuditOutboxRecord(1L, "event-1", "{\"eventId\":\"event-1\"}", 1),
                new AuditOutboxRecord(2L, "event-2", "{\"eventId\":\"event-2\"}", 1));
        when(repository.claim(eq(100), any(Instant.class), eq(Duration.ofMinutes(2))))
                .thenReturn(records);
        when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
                .thenReturn(PutRecordBatchResponse.builder()
                        .failedPutCount(1)
                        .requestResponses(
                                PutRecordBatchResponseEntry.builder().recordId("ok").build(),
                                PutRecordBatchResponseEntry.builder()
                                        .errorCode("ServiceUnavailableException")
                                        .errorMessage("retry")
                                        .build())
                        .build());

        ScheduledJobTelemetry.Outcome outcome = job.publishBatch();

        assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.PARTIAL_FAILURE);
        verify(repository).markPublished(eq(List.of(1L)), any(Instant.class));
        verify(repository).markFailed(eq(2L), any(Instant.class),
                eq("ServiceUnavailableException: retry"));
        verify(metrics).published(1);
        verify(metrics).failed(1);
    }

    @Test
    void networkFailureReturnsEveryClaimedRecordToFailed() {
        List<AuditOutboxRecord> records = List.of(
                new AuditOutboxRecord(7L, "event-7", "{}", 3),
                new AuditOutboxRecord(8L, "event-8", "{}", 3));
        when(repository.claim(eq(100), any(Instant.class), any(Duration.class)))
                .thenReturn(records);
        when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
                .thenThrow(new IllegalStateException("network down"));

        ScheduledJobTelemetry.Outcome outcome = job.publishBatch();

        assertThat(outcome).isEqualTo(ScheduledJobTelemetry.Outcome.FAILURE);
        verify(repository).markFailed(eq(7L), any(Instant.class), anyString());
        verify(repository).markFailed(eq(8L), any(Instant.class), anyString());
        verify(repository, never()).markPublished(any(), any());
        verify(metrics).failed(2);
    }

    @Test
    void retryDelayIsCapped() {
        properties.setBaseRetryDelay(Duration.ofSeconds(5));
        properties.setMaxRetryDelay(Duration.ofMinutes(1));

        Duration delay = job.retryDelay(30);

        assertThat(delay).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1));
    }
}
