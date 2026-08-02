package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.audit.chain.AuditChainHash;
import com.vetsoftware.app.infrastructure.audit.chain.AuditChainRepository;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.firehose.FirehoseClient;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchRequest;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponse;
import software.amazon.awssdk.services.firehose.model.PutRecordBatchResponseEntry;
import tools.jackson.databind.ObjectMapper;

class AuditOutboxPublisherJobTest {

  private final AuditOutboxRepository repository = mock(AuditOutboxRepository.class);
  private final AuditChainRepository chainRepository = mock(AuditChainRepository.class);
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
    job =
        new AuditOutboxPublisherJob(
            repository, chainRepository, properties, firehose, metrics, telemetry);
  }

  @Test
  void appliesPartialFirehoseResponsePerRecord() {
    List<AuditOutboxRecord> records =
        List.of(
            record(1L, "event-1", "{\"eventId\":\"event-1\"}", 1),
            record(2L, "event-2", "{\"eventId\":\"event-2\"}", 2));
    when(repository.claim(eq(100), any(Instant.class), eq(Duration.ofMinutes(2))))
        .thenReturn(records);
    when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
        .thenReturn(
            PutRecordBatchResponse.builder()
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
    verify(repository)
        .markFailed(eq(2L), any(Instant.class), eq("ServiceUnavailableException: retry"));
    verify(metrics).published(1);
    verify(metrics).failed(1);
  }

  @Test
  void networkFailureReturnsEveryClaimedRecordToFailed() {
    List<AuditOutboxRecord> records =
        List.of(record(7L, "event-7", "{}", 7), record(8L, "event-8", "{}", 8));
    when(repository.claim(eq(100), any(Instant.class), any(Duration.class))).thenReturn(records);
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
  void elRegistroEnviadoLlevaElBloqueDeIntegridad() {
    when(repository.claim(eq(100), any(Instant.class), any(Duration.class)))
        .thenReturn(List.of(record(1L, "event-1", "{\"event\":\"http_mutation\"}", 42)));
    when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
        .thenReturn(
            PutRecordBatchResponse.builder()
                .failedPutCount(0)
                .requestResponses(PutRecordBatchResponseEntry.builder().recordId("ok").build())
                .build());

    job.publishBatch();

    ArgumentCaptor<PutRecordBatchRequest> captor =
        ArgumentCaptor.forClass(PutRecordBatchRequest.class);
    verify(firehose).putRecordBatch(captor.capture());
    String line = captor.getValue().records().getFirst().data().asUtf8String();

    // Debe seguir siendo NDJSON válido y conservar los campos originales del evento.
    assertThat(line).endsWith("\n");
    assertThat(line).contains("\"event\":\"http_mutation\"");
    assertThat(line).contains("\"sequence\":42");
    assertThat(line).contains("\"chainHash\":\"" + hash("chain-42") + "\"");
    assertThatCode(() -> new ObjectMapper().readTree(line)).doesNotThrowAnyException();
  }

  @Test
  void unObjetoVacioNoProduceJsonInvalido() {
    when(repository.claim(eq(100), any(Instant.class), any(Duration.class)))
        .thenReturn(List.of(record(1L, "event-1", "{}", 1)));
    when(firehose.putRecordBatch(any(PutRecordBatchRequest.class)))
        .thenReturn(
            PutRecordBatchResponse.builder()
                .failedPutCount(0)
                .requestResponses(PutRecordBatchResponseEntry.builder().recordId("ok").build())
                .build());

    job.publishBatch();

    ArgumentCaptor<PutRecordBatchRequest> captor =
        ArgumentCaptor.forClass(PutRecordBatchRequest.class);
    verify(firehose).putRecordBatch(captor.capture());
    String line = captor.getValue().records().getFirst().data().asUtf8String();

    assertThat(line).doesNotContain(",}");
    assertThatCode(() -> new ObjectMapper().readTree(line)).doesNotThrowAnyException();
  }

  @Test
  void secuenciaAntesDePublicar() {
    when(repository.claim(eq(100), any(Instant.class), any(Duration.class))).thenReturn(List.of());

    job.publishBatch();

    // Sin secuenciar primero, un evento recién insertado nunca llegaría al archivo.
    verify(chainRepository)
        .sequencePending(eq(properties.getSequenceBatchSize()), any(Instant.class));
  }

  @Test
  void retryDelayIsCapped() {
    properties.setBaseRetryDelay(Duration.ofSeconds(5));
    properties.setMaxRetryDelay(Duration.ofMinutes(1));

    Duration delay = job.retryDelay(30);

    assertThat(delay).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1));
  }

  private static AuditOutboxRecord record(long id, String eventId, String payload, long sequence) {
    return new AuditOutboxRecord(
        id,
        eventId,
        payload,
        1,
        sequence,
        hash("prev-" + sequence),
        hash("payload-" + sequence),
        hash("chain-" + sequence));
  }

  /** Hexadecimal de 64 caracteres con la forma que exige el bloque de integridad. */
  private static String hash(String seed) {
    return AuditChainHash.payloadHash(seed);
  }
}
