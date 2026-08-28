package com.vetsoftware.app.subscription.application.usecase;

import static com.vetsoftware.app.subscription.testsupport.SubscriptionMother.CONTRATO;
import static com.vetsoftware.app.subscription.testsupport.SubscriptionMother.EMPRESA;
import static com.vetsoftware.app.subscription.testsupport.SubscriptionMother.contratoVigente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.RenewSubscriptionPeriodCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El llamador de produccion que a {@code Subscription.renewPeriod} le faltaba.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RenewSubscriptionPeriodService — el periodo facturado avanza")
class RenewSubscriptionPeriodServiceTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 2);
    private static final LocalDate FIN = LocalDate.of(2026, 4, 1);
    private static final LocalDate PROXIMO = LocalDate.of(2026, 4, 2);

    @Mock
    private SubscriptionRepository repository;
    @InjectMocks
    private RenewSubscriptionPeriodService service;

    private static RenewSubscriptionPeriodCommand comando() {
        return new RenewSubscriptionPeriodCommand(CONTRATO, EMPRESA, INICIO, FIN, PROXIMO);
    }

    @Nested
    @DisplayName("Avance")
    class Avance {

        @Test
        @DisplayName("mueve el periodo en curso y la fecha del proximo cobro")
        void mueve_el_periodo() {
            Subscription contrato = contratoVigente();
            when(repository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionDto dto = service.execute(comando());

            ArgumentCaptor<Subscription> guardado = ArgumentCaptor.forClass(Subscription.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getCurrentPeriodStart()).isEqualTo(INICIO);
            assertThat(guardado.getValue().getCurrentPeriodEnd()).isEqualTo(FIN);
            assertThat(guardado.getValue().getNextBillingDate()).isEqualTo(PROXIMO);
            assertThat(dto.currentPeriodStart()).isEqualTo(INICIO);
        }

        @Test
        @DisplayName("no toca nada de lo contratado: ni el estado ni la tarifa")
        void no_toca_lo_contratado() {
            Subscription contrato = contratoVigente();
            when(repository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contrato));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionDto dto = service.execute(comando());

            assertThat(dto.status()).isEqualTo(contrato.getStatus());
            assertThat(dto.priceListId()).isEqualTo(contrato.getPriceListId());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("exige el contrato")
        void exige_el_contrato() {
            assertThatThrownBy(() -> service.execute(
                    new RenewSubscriptionPeriodCommand(null, EMPRESA, INICIO, FIN, PROXIMO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscriptionId is required");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("exige la empresa")
        void exige_la_empresa() {
            assertThatThrownBy(() -> service.execute(
                    new RenewSubscriptionPeriodCommand(CONTRATO, null, INICIO, FIN, PROXIMO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        /**
         * La comprobacion vive en el dominio y no se repite aqui: una sola version de
         * esa regla es lo que impide que las dos se desincronicen.
         */
        @Test
        @DisplayName("un periodo invertido lo rechaza el dominio y no se guarda")
        void periodo_invertido() {
            when(repository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoVigente()));

            assertThatThrownBy(() -> service.execute(
                    new RenewSubscriptionPeriodCommand(CONTRATO, EMPRESA, FIN, INICIO, PROXIMO)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentPeriodEnd must not be before");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        /**
         * El puerto no declara ninguna carga por id sin empresa, asi que el contrato de
         * otra clinica simplemente no se resuelve: no hay forma de avanzarle el periodo
         * a un tenant ajeno.
         */
        @Test
        @DisplayName("un contrato que no es de esa empresa no se resuelve y no se guarda")
        void contrato_de_otra_empresa() {
            when(repository.lockByIdAndCompanyId(CONTRATO, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(SubscriptionNotFoundException.class)
                    .hasMessageContaining("Subscription not found: " + CONTRATO);

            verify(repository, never()).save(any());
        }
    }
}
