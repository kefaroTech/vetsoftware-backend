package com.vetsoftware.app.subscriptionpaymentmethod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpaymentmethod.application.command.SetDefaultPaymentMethodCommand;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.PaymentRetryTriggerPort;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.StalledPaymentRetryQueryPort;
import com.vetsoftware.app.subscriptionpaymentmethod.application.port.out.SubscriptionPaymentMethodRepository;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethod;
import com.vetsoftware.app.subscriptionpaymentmethod.domain.SubscriptionPaymentMethodNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SetDefaultPaymentMethodService")
class SetDefaultPaymentMethodServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long MEDIO = 10L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 8, 0);

    @Mock
    private SubscriptionPaymentMethodRepository repository;
    @Mock
    private StalledPaymentRetryQueryPort stalledPaymentRetryQueryPort;
    @Mock
    private PaymentRetryTriggerPort paymentRetryTriggerPort;

    private SetDefaultPaymentMethodService service;

    @BeforeEach
    void montar() {
        service = new SetDefaultPaymentMethodService(repository, stalledPaymentRetryQueryPort,
                paymentRetryTriggerPort);
    }

    private static SubscriptionPaymentMethod tarjetaActiva() {
        return SubscriptionPaymentMethod.register(EMPRESA, PaymentMethodKind.CARD, "wompi",
                "tok_nuevo", "VISA", "4242", LocalDate.of(2030, 1, 1), "Mandato firmado", AHORA,
                AHORA);
    }

    private void sinDocumentosVarados() {
        when(stalledPaymentRetryQueryPort.findStalledLastAttemptIds(EMPRESA)).thenReturn(List.of());
    }

    @Nested
    @DisplayName("Marcar predeterminado")
    class MarcarPredeterminado {

        @Test
        @DisplayName("limpia el predeterminado anterior y guarda el nuevo")
        void limpia_y_guarda() {
            when(repository.findByIdAndCompanyId(MEDIO, EMPRESA))
                    .thenReturn(Optional.of(tarjetaActiva()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            sinDocumentosVarados();

            SubscriptionPaymentMethodDto dto = service
                    .execute(new SetDefaultPaymentMethodCommand(MEDIO, EMPRESA));

            verify(repository).clearDefaultForCompany(EMPRESA, MEDIO);
            assertThat(dto.defaultMethod()).isTrue();
        }

        @Test
        @DisplayName("un medio inexistente no reactiva ningun reintento")
        void un_medio_inexistente_no_reactiva_nada() {
            when(repository.findByIdAndCompanyId(MEDIO, EMPRESA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new SetDefaultPaymentMethodCommand(MEDIO, EMPRESA)))
                    .isInstanceOf(SubscriptionPaymentMethodNotFoundException.class);

            verifyNoInteractions(stalledPaymentRetryQueryPort, paymentRetryTriggerPort);
        }
    }

    @Nested
    @DisplayName("Reactivacion de reintentos varados (RES-45)")
    class ReactivacionDeReintentosVarados {

        @Test
        @DisplayName("reprograma a ahora cada ultimo intento varado (CONFIGURATION o HARD) con saldo")
        void reprograma_los_varados() {
            when(repository.findByIdAndCompanyId(MEDIO, EMPRESA))
                    .thenReturn(Optional.of(tarjetaActiva()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(stalledPaymentRetryQueryPort.findStalledLastAttemptIds(EMPRESA))
                    .thenReturn(List.of(701L, 900L));

            service.execute(new SetDefaultPaymentMethodCommand(MEDIO, EMPRESA));

            verify(paymentRetryTriggerPort).rescheduleNow(EMPRESA, 701L);
            verify(paymentRetryTriggerPort).rescheduleNow(EMPRESA, 900L);
        }

        @Test
        @DisplayName("sin documentos varados no dispara ningun reintento")
        void sin_documentos_varados_no_dispara_nada() {
            when(repository.findByIdAndCompanyId(MEDIO, EMPRESA))
                    .thenReturn(Optional.of(tarjetaActiva()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            sinDocumentosVarados();

            service.execute(new SetDefaultPaymentMethodCommand(MEDIO, EMPRESA));

            verifyNoInteractions(paymentRetryTriggerPort);
        }
    }
}
