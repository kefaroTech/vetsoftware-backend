package com.vetsoftware.app.paymentgateway.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.paymentgateway.domain.GatewayWebhookOutcome;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaGatewayWebhookEventRecorderPort")
class JpaGatewayWebhookEventRecorderPortTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 4, 10, 0);

    @Mock
    private WompiWebhookEventJpaRepository repository;
    @InjectMocks
    private JpaGatewayWebhookEventRecorderPort port;

    @Test
    @DisplayName("recordReceived persiste el evento sin procesar y devuelve el id generado")
    void recordReceived_persiste_sin_procesar() {
        when(repository.save(any())).thenAnswer(inv -> {
            WompiWebhookEventJpaEntity entity = inv.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        Long id = port.recordReceived("WOMPI", "transaction.updated", "checksum-1", "tx-1", AHORA,
                "{}");

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<WompiWebhookEventJpaEntity> captor = ArgumentCaptor
                .forClass(WompiWebhookEventJpaEntity.class);
        verify(repository).save(captor.capture());
        WompiWebhookEventJpaEntity guardado = captor.getValue();
        assertThat(guardado.getGateway()).isEqualTo("WOMPI");
        assertThat(guardado.getEventChecksum()).isEqualTo("checksum-1");
        assertThat(guardado.getGatewayReference()).isEqualTo("tx-1");
        assertThat(guardado.getRawBody()).isEqualTo("{}");
        assertThat(guardado.getProcessedAt()).isNull();
        assertThat(guardado.getProcessingOutcome()).isNull();
    }

    @Test
    @DisplayName("recordOutcome carga la fila existente y rellena procesado y desenlace")
    void recordOutcome_rellena_procesado_y_desenlace() {
        WompiWebhookEventJpaEntity existente = new WompiWebhookEventJpaEntity();
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        port.recordOutcome(1L, AHORA, GatewayWebhookOutcome.APPLIED);

        assertThat(existente.getProcessedAt()).isEqualTo(AHORA);
        assertThat(existente.getProcessingOutcome()).isEqualTo("APPLIED");
        verify(repository).save(existente);
    }

    @Test
    @DisplayName("recordOutcome sobre un id inexistente revienta en vez de crear una fila nueva")
    void recordOutcome_id_inexistente_revienta() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> port.recordOutcome(99L, AHORA, GatewayWebhookOutcome.APPLIED))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("99");
    }

    @Test
    @DisplayName("purgeRawBodyOlderThan delega en el repositorio y devuelve cuantas filas toco")
    void purgeRawBodyOlderThan_delega_en_el_repositorio() {
        when(repository.purgeRawBodyReceivedBefore(AHORA)).thenReturn(3);

        int purgadas = port.purgeRawBodyOlderThan(AHORA);

        assertThat(purgadas).isEqualTo(3);
        verify(repository).purgeRawBodyReceivedBefore(AHORA);
    }
}
