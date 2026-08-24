package com.vetsoftware.app.subscription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Los dos DTO del slice que tienen algo que decidir. El resto son proyecciones
 * campo a campo y quedan ejercitados por las rodajas de web y por los
 * servicios; duplicarlos aquí sería escribir tests para mover el número.
 */
@DisplayName("DTO de subscription")
class SubscriptionDtoTest {

    @Nested
    @DisplayName("La peticion de baja se aplana en tres campos, o en tres nulos")
    class PeticionDeBaja {

        @Test
        @DisplayName("un contrato sin baja pedida sale con los tres campos nulos")
        void sinBajaPedida() {
            SubscriptionDto dto = SubscriptionDto.from(SubscriptionMother.contratoVigente());

            assertThat(dto.cancelRequestedAt()).isNull();
            assertThat(dto.cancelEffectiveDate()).isNull();
            assertThat(dto.cancelReason()).isNull();
            assertThat(dto.current()).isTrue();
        }

        @Test
        @DisplayName("con la baja pedida salen las dos fechas y el motivo, y sigue vigente")
        void conBajaPedida() {
            // Las dos fechas van separadas a proposito: cuando lo pidio y desde cuando
            // aplica. Aplanarlas en una sola haria imposible responder «¿cuando aviso
            // el cliente?», que es la pregunta de cualquier reclamacion de cobro.
            Subscription contrato = SubscriptionMother.contratoVigente();
            LocalDateTime pedida = LocalDateTime.of(2026, 1, 10, 9, 30);
            contrato.requestCancellation(pedida, LocalDate.of(2026, 1, 30), "Se va");

            SubscriptionDto dto = SubscriptionDto.from(contrato);

            assertThat(dto.cancelRequestedAt()).isEqualTo(pedida);
            assertThat(dto.cancelEffectiveDate()).isEqualTo(LocalDate.of(2026, 1, 30));
            assertThat(dto.cancelReason()).isEqualTo("Se va");
            // Sigue vigente: lo que el cliente ya pago se disfruta hasta el 30.
            assertThat(dto.current()).isTrue();
            assertThat(dto.autoRenew()).isFalse();
        }
    }

    @Nested
    @DisplayName("El resultado de un lote del barrido")
    class ResultadoDelLote {

        @Test
        @DisplayName("un lote vacio es cero procesados y cursor cero, no un nulo")
        void loteVacio() {
            SubscriptionLifecycleBatchResult lote = new SubscriptionLifecycleBatchResult(0, 0L);

            assertThat(lote.processed()).isZero();
            assertThat(lote.lastId()).isZero();
        }

        @ParameterizedTest
        @CsvSource({"-1, 0, processed", "0, -1, lastId"})
        @DisplayName("ni el conteo ni el cursor pueden ser negativos")
        void nadaNegativo(int processed, long lastId, String campo) {
            // Un cursor negativo haria que la vuelta siguiente del barrido volviera a
            // procesar contratos ya procesados, y un conteo negativo restaria del total
            // que decide si el job reporta trabajo o no_work.
            assertThatThrownBy(() -> new SubscriptionLifecycleBatchResult(processed, lastId))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(campo);
        }
    }
}
