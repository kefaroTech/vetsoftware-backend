package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionNumberPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionItemRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.subscription.domain.AmendmentType;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionAmendment;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("RemoveSubscriptionItemService - dar de baja no borra")
class RemoveSubscriptionItemServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long LINEA = 500L;
    private static final Long ARTICULO = 100L;
    private static final String LLAVE = "req-baja-1";
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JUNIO_30 = LocalDate.of(2026, 6, 30);

    @Mock
    private SubscriptionRepository subscriptionRepository;
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
    private RemoveSubscriptionItemService service;

    private static Subscription contrato() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null,
                null, 0, null, true, null, null, 0L, true);
    }

    private static SubscriptionItem lineaAbierta() {
        return new SubscriptionItem(LINEA, EMPRESA, CONTRATO, ARTICULO, "VET", "Veterinaria",
                SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED, 1,
                new BigDecimal("179000.00"), BigDecimal.ZERO, EffectivePeriod.openFrom(ENERO_1),
                ItemOrigin.ADDON, 11L, null, null, 0L, true);
    }

    private static RemoveSubscriptionItemCommand comando() {
        return new RemoveSubscriptionItemCommand(CONTRATO, EMPRESA, LINEA, LLAVE, JUNIO_30,
                "Ya no lo usa", null, 900L);
    }

    private static SubscriptionAmendment otrosiGuardado() {
        return new SubscriptionAmendment(901L, EMPRESA, CONTRATO, "AMD-2026-0002",
                AmendmentType.REMOVE_ITEM, JUNIO_30, null, null, 900L, BigDecimal.ZERO,
                BigDecimal.ZERO, null, LLAVE, null);
    }

    private void escenarioFeliz() {
        when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(contrato()));
        when(itemRepository.findByIdAndCompanyId(LINEA, EMPRESA))
                .thenReturn(Optional.of(lineaAbierta()));
        when(amendmentRepository.save(any())).thenReturn(otrosiGuardado());
        when(itemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
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
    @DisplayName("No se destruye nada")
    class NoSeDestruyeNada {

        @Test
        @DisplayName("solo escribe effective_to y el otrosi que la cerro")
        void soloEscribeLaFechaDeFin() {
            escenarioFeliz();

            SubscriptionItemDto resultado = service.execute(comando());

            assertThat(resultado.effectiveTo()).isEqualTo(JUNIO_30);
            assertThat(resultado.effectiveFrom()).isEqualTo(ENERO_1);
            assertThat(resultado.endedAmendmentId()).isEqualTo(901L);
        }

        @Test
        @DisplayName("la fila sigue habilitada: no se desactiva ni se borra")
        void laFilaSigueViva() {
            escenarioFeliz();

            service.execute(comando());

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository).save(captor.capture());
            // R12: la informacion de que este cliente tuvo este modulo es legalmente
            // suya. Si se borra, no tiene vuelta atras.
            assertThat(captor.getValue().isEnabled()).isTrue();
        }

        @Test
        @DisplayName("conserva intactos el precio y lo incluido de la linea cerrada")
        void conservaLoCongelado() {
            escenarioFeliz();

            SubscriptionItemDto resultado = service.execute(comando());

            assertThat(resultado.unitAmount()).isEqualByComparingTo("179000.00");
            assertThat(resultado.includedQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("anuncia el cambio para que el recalculo baje el acceso")
        void anunciaElCambio() {
            escenarioFeliz();

            service.execute(comando());

            // Lo que baja el acceso a READ_ONLY es el recalculo del slice entitlement,
            // no este caso de uso: aqui solo se anuncia.
            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.ITEM_REMOVED, JUNIO_30));
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("el reintento devuelve la linea ya cerrada y no emite otro otrosi")
        void reintentoNoDuplica() {
            SubscriptionItem yaCerrada = new SubscriptionItem(LINEA, EMPRESA, CONTRATO, ARTICULO,
                    "VET", "Veterinaria", SubscriptionItemType.MODULE, null, 2, TaxTreatment.TAXED,
                    1, new BigDecimal("179000.00"), BigDecimal.ZERO,
                    new EffectivePeriod(ENERO_1, JUNIO_30), ItemOrigin.ADDON, 11L, 901L, null, 1L,
                    true);
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.of(otrosiGuardado()));
            when(itemRepository.findByIdAndCompanyId(LINEA, EMPRESA))
                    .thenReturn(Optional.of(yaCerrada));

            SubscriptionItemDto resultado = service.execute(comando());

            assertThat(resultado.effectiveTo()).isEqualTo(JUNIO_30);
            verify(amendmentRepository, never()).save(any());
            verify(itemRepository, never()).save(any());
            verify(subscriptionRepository, never()).lockByIdAndCompanyId(anyLong(), anyLong());
        }
    }
}
