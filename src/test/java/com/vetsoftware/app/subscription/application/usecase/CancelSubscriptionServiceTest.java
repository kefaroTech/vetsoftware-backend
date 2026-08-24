package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EmployeeRef;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelSubscriptionService - cuando lo pidio vs cuando se va")
class CancelSubscriptionServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final String LLAVE = "req-cancel-1";
    private static final LocalDateTime DIA_10 = LocalDateTime.of(2026, 1, 10, 9, 30);
    private static final LocalDate DIA_30 = LocalDate.of(2026, 1, 30);

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private SubscriptionItemRepository itemRepository;
    @Mock
    private SubscriptionAmendmentRepository amendmentRepository;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @Mock
    private SystemUserValidationPort systemUserValidationPort;
    @Mock
    private SubscriptionNumberPort subscriptionNumberPort;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;

    @InjectMocks
    private CancelSubscriptionService service;

    private static Subscription contratoActivo() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 1, 1), null, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31), null, null, 0, null, true, null, null, 0L, true);
    }

    private static CancelSubscriptionCommand comando() {
        return new CancelSubscriptionCommand(CONTRATO, EMPRESA, DIA_10, DIA_30,
                "Se paso a la competencia", LLAVE, 55L, null);
    }

    private static SubscriptionAmendment otrosiGuardado() {
        return new SubscriptionAmendment(909L, EMPRESA, CONTRATO, "AMD-2026-0009",
                AmendmentType.CANCEL, DIA_30, null, 55L, null, BigDecimal.ZERO, BigDecimal.ZERO,
                null, LLAVE, null);
    }

    @BeforeEach
    void elConsecutivoLoReservaElServidor() {
        // El numero del otrosi ya no viaja en el command: lo reserva el puerto dentro
        // de la transaccion. Leniente porque los caminos que rechazan antes —solape,
        // reintento idempotente, empleado ajeno— no llegan a pedirlo.
        lenient().when(subscriptionNumberPort.nextAmendmentNumber(anyInt()))
                .thenReturn("AMD-2026-00001");
    }

    @Nested
    @DisplayName("Cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("no corta el servicio: el contrato sigue vigente hasta la fecha efectiva")
        void noCortaElServicio() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoActivo()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            SubscriptionDto resultado = service.execute(comando());

            assertThat(resultado.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(resultado.current()).isTrue();
        }

        @Test
        @DisplayName("guarda las dos fechas por separado y el motivo")
        void guardaLasDosFechas() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoActivo()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            SubscriptionDto resultado = service.execute(comando());

            assertThat(resultado.cancelRequestedAt()).isEqualTo(DIA_10);
            assertThat(resultado.cancelEffectiveDate()).isEqualTo(DIA_30);
            assertThat(resultado.cancelReason()).isEqualTo("Se paso a la competencia");
            assertThat(resultado.autoRenew()).isFalse();
        }

        @Test
        @DisplayName("emite el otrosi de tipo CANCEL con los importes que calcula el servidor")
        void emiteElOtrosiDeCancelacion() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoActivo()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
            // Una sola linea vigente de 179.000 al mes: es la cuota que deja de
            // facturarse.
            when(itemRepository.findAllCurrentOn(CONTRATO, EMPRESA, DIA_30))
                    .thenReturn(List.of(SubscriptionMother.moduloAbierto()));

            service.execute(comando());

            ArgumentCaptor<SubscriptionAmendment> captor = ArgumentCaptor
                    .forClass(SubscriptionAmendment.class);
            verify(amendmentRepository).save(captor.capture());
            SubscriptionAmendment otrosi = captor.getValue();
            assertThat(otrosi.getAmendmentType()).isEqualTo(AmendmentType.CANCEL);
            // Los importes los calcula el servidor. La cuota recurrente cae entera; el
            // abono es solo por los dias del periodo que el cliente pago y no va a
            // consumir: el 30 y el 31 de enero, 2 de 31 dias sobre 179.000.
            assertThat(otrosi.getMonthlyDeltaAmount()).isEqualByComparingTo("-179000.00");
            assertThat(otrosi.getProrationAmount()).isEqualByComparingTo("-11548.39");
        }

        @Test
        @DisplayName("anuncia la peticion de baja para el recalculo")
        void anunciaLaPeticion() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.empty());
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoActivo()));
            when(employeeQueryPort.findByIdAndCompanyId(55L, EMPRESA))
                    .thenReturn(Optional.of(new EmployeeRef(55L, "Ana")));
            when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.execute(comando());

            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.CANCELLATION_REQUESTED, DIA_30));
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("dos clics en Cancelar no emiten dos otrosies de baja")
        void dosClicsUnSoloOtrosi() {
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.of(otrosiGuardado()));
            when(repository.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(contratoActivo()));

            SubscriptionDto resultado = service.execute(comando());

            assertThat(resultado.id()).isEqualTo(CONTRATO);
            verify(amendmentRepository, never()).save(any());
            verify(repository, never()).save(any());
        }
    }
}
