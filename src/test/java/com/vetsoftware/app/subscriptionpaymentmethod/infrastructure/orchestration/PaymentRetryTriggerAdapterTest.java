package com.vetsoftware.app.subscriptionpaymentmethod.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.paymentattempt.application.command.ReschedulePaymentAttemptCommand;
import com.vetsoftware.app.paymentattempt.application.port.in.ReschedulePaymentAttemptUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRetryTriggerAdapter")
class PaymentRetryTriggerAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 20, 8, 0);
    private static final Clock CLOCK = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private ReschedulePaymentAttemptUseCase rescheduleUseCase;

    private PaymentRetryTriggerAdapter adapter;

    @BeforeEach
    void montar() {
        adapter = new PaymentRetryTriggerAdapter(rescheduleUseCase, new SystemAuthRunner(), CLOCK);
    }

    @Test
    @DisplayName("reprograma el intento a la hora exacta del reloj")
    void reprograma_el_intento_a_ahora() {
        adapter.rescheduleNow(EMPRESA, 501L);

        verify(rescheduleUseCase)
                .execute(new ReschedulePaymentAttemptCommand(501L, EMPRESA, AHORA));
    }
}
