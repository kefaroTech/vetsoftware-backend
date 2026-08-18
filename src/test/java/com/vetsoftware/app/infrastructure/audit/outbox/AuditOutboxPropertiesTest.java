package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditOutboxPropertiesTest {

    @Test
    @DisplayName("las propiedades por defecto son validas")
    void las_propiedades_por_defecto_son_validas() {
        assertThatCode(new AuditOutboxProperties()::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza un cleanup-batch-size no positivo")
    void rechaza_un_cleanup_batch_size_no_positivo() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setCleanupBatchSize(0);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cleanup-batch-size debe ser positivo");
    }

    @Test
    @DisplayName("rechaza un sequence-batch-size no positivo")
    void rechaza_un_sequence_batch_size_no_positivo() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setSequenceBatchSize(0);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "sequence-batch-size y verify-batch-size deben ser positivos");
    }

    @Test
    @DisplayName("rechaza un verify-batch-size no positivo")
    void rechaza_un_verify_batch_size_no_positivo() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setVerifyBatchSize(0);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "sequence-batch-size y verify-batch-size deben ser positivos");
    }

    @Test
    @DisplayName("rechaza un sequence-batch-size menor que el batch-size del publicador")
    void rechaza_un_sequence_batch_size_menor_que_el_batch_size() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setBatchSize(200);
        properties.setSequenceBatchSize(50);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sequence-batch-size no puede ser menor que batch-size");
    }

    @Test
    @DisplayName("rechaza una lease-duration no positiva")
    void rechaza_una_lease_duration_no_positiva() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setLeaseDuration(Duration.ZERO);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duraciones inválidas");
    }

    @Test
    @DisplayName("rechaza una base-retry-delay no positiva")
    void rechaza_una_base_retry_delay_no_positiva() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setBaseRetryDelay(Duration.ZERO);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duraciones inválidas");
    }

    @Test
    @DisplayName("rechaza una max-retry-delay menor que la base-retry-delay")
    void rechaza_una_max_retry_delay_menor_que_la_base() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setBaseRetryDelay(Duration.ofMinutes(5));
        properties.setMaxRetryDelay(Duration.ofMinutes(1));

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duraciones inválidas");
    }

    @Test
    @DisplayName("rechaza una retention no positiva")
    void rechaza_una_retention_no_positiva() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setRetention(Duration.ZERO);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duraciones inválidas");
    }

    @Test
    void rejectsBatchLargerThanFirehoseLimit() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setBatchSize(501);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("entre 1 y 500");
    }

    @Test
    void requiresStreamWhenPublisherIsEnabled() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setPublisherEnabled(true);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("delivery-stream-name");
    }

    @Test
    void rejectsIncompleteStaticCredentials() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setAccessKey("test");

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deben configurarse juntos");
    }

    @Test
    @DisplayName("rechaza un secret-key sin su access-key correspondiente")
    void rechaza_un_secret_key_sin_su_access_key() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setSecretKey("solo-secret");

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deben configurarse juntos");
    }

    @Test
    @DisplayName("acepta access-key y secret-key configurados juntos")
    void acepta_access_key_y_secret_key_configurados_juntos() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setAccessKey("clave-de-acceso");
        properties.setSecretKey("clave-secreta");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("acepta el publicador habilitado con un delivery-stream-name resuelto")
    void acepta_el_publicador_habilitado_con_un_delivery_stream_resuelto() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setPublisherEnabled(true);
        properties.setDeliveryStreamName("audit-stream");

        assertThatCode(properties::validate).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rechaza un delivery-stream-name que quedó con un placeholder sin resolver")
    void rechaza_un_delivery_stream_name_con_placeholder_sin_resolver() {
        AuditOutboxProperties properties = new AuditOutboxProperties();
        properties.setPublisherEnabled(true);
        properties.setDeliveryStreamName("${AUDIT_FIREHOSE_DELIVERY_STREAM}");

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quedó con un placeholder sin resolver");
    }
}
