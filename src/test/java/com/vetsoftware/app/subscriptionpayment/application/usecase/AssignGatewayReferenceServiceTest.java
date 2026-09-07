package com.vetsoftware.app.subscriptionpayment.application.usecase;

import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.EMPRESA;
import static com.vetsoftware.app.subscriptionpayment.testsupport.SubscriptionPaymentMother.reservaSinReferencia;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionpayment.application.command.AssignGatewayReferenceCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.application.port.out.SubscriptionPaymentRepository;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPayment;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentNotFoundException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignGatewayReferenceService")
class AssignGatewayReferenceServiceTest {

    @Mock
    private SubscriptionPaymentRepository repository;

    private AssignGatewayReferenceService service;

    @Test
    @DisplayName("bloquea por id y empresa, asigna la referencia y guarda")
    void asigna_y_guarda_bajo_candado() {
        service = new AssignGatewayReferenceService(repository);
        SubscriptionPayment reserva = reservaSinReferencia();
        when(repository.lockByIdAndCompanyId(10L, EMPRESA)).thenReturn(Optional.of(reserva));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionPaymentDto dto = service
                .execute(new AssignGatewayReferenceCommand(10L, EMPRESA, "TX-2026-0001", null));

        assertThat(dto.gatewayReference()).isEqualTo("TX-2026-0001");
        ArgumentCaptor<SubscriptionPayment> captor = ArgumentCaptor
                .forClass(SubscriptionPayment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getGatewayReference()).isEqualTo("TX-2026-0001");
    }

    @Test
    @DisplayName("con la fecha de la pasarela, sustituye receivedAt por la de la transaccion real"
            + " (#783)")
    void sustituye_received_at_por_la_fecha_de_la_pasarela() {
        service = new AssignGatewayReferenceService(repository);
        SubscriptionPayment reserva = reservaSinReferencia();
        LocalDateTime fechaTransaccion = LocalDateTime.of(2026, 9, 1, 14, 30);
        when(repository.lockByIdAndCompanyId(10L, EMPRESA)).thenReturn(Optional.of(reserva));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(
                new AssignGatewayReferenceCommand(10L, EMPRESA, "TX-2026-0001", fechaTransaccion));

        ArgumentCaptor<SubscriptionPayment> captor = ArgumentCaptor
                .forClass(SubscriptionPayment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getReceivedAt()).isEqualTo(fechaTransaccion);
    }

    @Test
    @DisplayName("pago inexistente para esa empresa: no guarda nada")
    void pago_inexistente_no_guarda() {
        service = new AssignGatewayReferenceService(repository);
        when(repository.lockByIdAndCompanyId(10L, EMPRESA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service
                .execute(new AssignGatewayReferenceCommand(10L, EMPRESA, "TX-1", null)))
                .isInstanceOf(SubscriptionPaymentNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
