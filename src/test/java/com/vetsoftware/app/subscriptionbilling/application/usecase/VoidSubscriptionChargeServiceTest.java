package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.VoidSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionChargeNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoidSubscriptionChargeService — anular es compensar, y los dos quedan")
class VoidSubscriptionChargeServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));

    @Mock
    private SubscriptionChargeRepository repository;

    private VoidSubscriptionChargeService service;

    @BeforeEach
    void setUp() {
        service = new VoidSubscriptionChargeService(repository, RELOJ);
    }

    private static SubscriptionCharge original() {
        return new SubscriptionCharge(500L, EMPRESA, 7L, 3L, ChargeType.RECURRING,
                "Plan CORE agosto", AGOSTO, BigDecimal.ONE, new BigDecimal("179000.00"),
                new BigDecimal("179000.00"), new BigDecimal("19.00"), TaxTreatment.TAXED, null,
                ChargeStatus.PENDING, null, null, null, LocalDateTime.of(2026, 8, 1, 0, 0));
    }

    @Nested
    @DisplayName("Convencion de signos — TRAMPA 1")
    class ConvencionDeSignos {

        @Test
        @DisplayName("guarda los DOS cargos: el original marcado VOIDED y el negativo"
                + " que lo compensa, encadenado por voidsChargeId")
        void guarda_los_dos() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(original()));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            SubscriptionChargeDto dto = service.execute(new VoidSubscriptionChargeCommand(500L,
                    EMPRESA, "Anulacion de la cuota de agosto"));

            ArgumentCaptor<SubscriptionCharge> guardados = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(repository, times(2)).save(guardados.capture());
            List<SubscriptionCharge> filas = guardados.getAllValues();

            assertThat(filas.get(0).getStatus()).isEqualTo(ChargeStatus.VOIDED);
            assertThat(filas.get(0).getSubtotalAmount()).isEqualByComparingTo("179000.00");
            assertThat(filas.get(1).getSubtotalAmount()).isEqualByComparingTo("-179000.00");
            assertThat(filas.get(1).getVoidsChargeId()).isEqualTo(500L);
            assertThat(filas.get(0).getSubtotalAmount().add(filas.get(1).getSubtotalAmount()))
                    .isEqualByComparingTo("0.00");
            assertThat(dto.chargeType()).isEqualTo(ChargeType.CREDIT);
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("-179000.00");
        }

        @Test
        @DisplayName("devuelve el cargo NUEVO, que es la fila que acaba de nacer")
        void devuelve_la_compensacion() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA))
                    .thenReturn(Optional.of(original()));
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            SubscriptionChargeDto dto = service
                    .execute(new VoidSubscriptionChargeCommand(500L, EMPRESA, "x"));

            assertThat(dto.voidsChargeId()).isEqualTo(500L);
            assertThat(dto.status()).isEqualTo(ChargeStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un cargo de otra empresa no existe, y no se escribe nada")
        void cargo_ajeno_no_existe() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new VoidSubscriptionChargeCommand(500L, EMPRESA, "x")))
                    .isInstanceOf(SubscriptionChargeNotFoundException.class);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la carga va SIEMPRE por la variante acotada por empresa")
        void siempre_por_la_variante_acotada() {
            when(repository.findByIdAndCompanyId(500L, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new VoidSubscriptionChargeCommand(500L, EMPRESA, "x")))
                    .isInstanceOf(SubscriptionChargeNotFoundException.class);

            verify(repository).findByIdAndCompanyId(500L, EMPRESA);
        }
    }
}
