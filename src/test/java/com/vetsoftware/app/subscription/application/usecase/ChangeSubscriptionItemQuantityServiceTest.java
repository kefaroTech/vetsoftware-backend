package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAmendmentRepository;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionAuditPort;
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
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemOverlapException;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@DisplayName("ChangeSubscriptionItemQuantityService - cerrar y abrir, nunca editar")
class ChangeSubscriptionItemQuantityServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long LINEA = 500L;
    private static final Long ARTICULO = 100L;
    private static final String LLAVE = "req-cantidad-1";
    private static final LocalDate ENERO_1 = LocalDate.of(2026, 1, 1);
    /**
     * Fecha efectiva del cambio, <b>dentro del periodo en curso</b> del contrato de
     * prueba (1..31 de enero de 2026).
     *
     * <p>
     * <b>Antes era el 30 de junio, y ese fixture afirmaba un defecto.</b> Un cambio
     * efectivo fuera del periodo en curso solo puede darse si el periodo se quedo
     * congelado -que es justo lo que pasaba, porque nada llamaba a
     * {@code Subscription.renewPeriod}-. El prorrateo salia cero y se guardaba en
     * el otrosi como si fuera un importe: firmado, inmutable y con toda la pinta de
     * estar bien. Hoy {@code ProrationCalculator} rechaza el prorrateo de cero
     * dias, asi que la fecha efectiva vuelve a caer donde la produce el negocio.
     */
    private static final LocalDate ENERO_17 = LocalDate.of(2026, 1, 17);
    private static final BigDecimal PRECIO_FIRMADO = new BigDecimal("15000.00");

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
    @Mock
    private SubscriptionAuditPort audit;

    @InjectMocks
    private ChangeSubscriptionItemQuantityService service;

    private static Subscription contrato() {
        return new Subscription(CONTRATO, "SUS-2026-00184", EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, ENERO_1, null, ENERO_1, LocalDate.of(2026, 1, 31), null,
                null, 0, null, true, null, null, 0L, true);
    }

    /**
     * Cinco usuarios contratados hace un ano, con dos incluidos y precio de
     * entonces.
     */
    private static SubscriptionItem lineaFirmadaHaceUnAno() {
        return new SubscriptionItem(LINEA, EMPRESA, CONTRATO, ARTICULO, "EXTRA_USER",
                "Usuario adicional", SubscriptionItemType.CAPACITY, "USER", 2, TaxTreatment.TAXED,
                5, PRECIO_FIRMADO, new BigDecimal("19.00"), EffectivePeriod.openFrom(ENERO_1),
                ItemOrigin.INITIAL, 11L, null, null, 0L, true);
    }

    private static ChangeSubscriptionItemQuantityCommand comando(int nuevaCantidad) {
        return new ChangeSubscriptionItemQuantityCommand(CONTRATO, EMPRESA, LINEA, nuevaCantidad,
                LLAVE, ENERO_17, "Contrataron dos personas mas", null, 900L);
    }

    private static SubscriptionAmendment otrosiGuardado() {
        return new SubscriptionAmendment(902L, EMPRESA, CONTRATO, "AMD-2026-0003",
                AmendmentType.CHANGE_QUANTITY, ENERO_17, null, null, 900L, BigDecimal.ZERO,
                BigDecimal.ZERO, null, LLAVE, null);
    }

    private void escenarioFeliz() {
        when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.lockByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(contrato()));
        when(itemRepository.findByIdAndCompanyId(LINEA, EMPRESA))
                .thenReturn(Optional.of(lineaFirmadaHaceUnAno()));
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
    @DisplayName("Congelacion")
    class Congelacion {

        @Test
        @DisplayName("la sucesora hereda el precio firmado, no uno nuevo")
        void heredaElPrecioFirmado() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            SubscriptionItemDto sucesora = service.execute(comando(7));

            assertThat(sucesora.unitAmount()).isEqualByComparingTo(PRECIO_FIRMADO);
        }

        @Test
        @DisplayName("la sucesora hereda included_quantity: editar la tarifa no le quita lo suyo")
        void heredaLoIncluido() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            SubscriptionItemDto sucesora = service.execute(comando(7));

            // Firmo hace un ano con 2 usuarios incluidos. Aunque la tarifa de hoy
            // incluya 0, a el se le siguen descontando 2: 7 - 2 = 5 facturables.
            assertThat(sucesora.includedQuantity()).isEqualTo(2);
            assertThat(sucesora.billableQuantity()).isEqualTo(5);
        }

        @Test
        @DisplayName("cierra la original y abre la sucesora: dos filas, ninguna editada")
        void cierraYAbre() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            service.execute(comando(7));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository, times(2)).save(captor.capture());

            SubscriptionItem original = captor.getAllValues().get(0);
            SubscriptionItem sucesora = captor.getAllValues().get(1);

            assertThat(original.getId()).isEqualTo(LINEA);
            assertThat(original.getQuantity()).isEqualTo(5);
            assertThat(original.getPeriod().to()).isEqualTo(ENERO_17);
            assertThat(original.getEndedAmendmentId()).isEqualTo(902L);

            assertThat(sucesora.getId()).isNull();
            assertThat(sucesora.getQuantity()).isEqualTo(7);
            assertThat(sucesora.getOrigin()).isEqualTo(ItemOrigin.QUANTITY_CHANGE);
            assertThat(sucesora.getCreatedAmendmentId()).isEqualTo(902L);
        }

        @Test
        @DisplayName("audita de cuanto venia, a cuanto va y cuanto sube la cuota")
        void auditaDeCuantoAcuantoYelDelta() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            service.execute(comando(7));

            // «Paso a 7» no es auditable sin el 5 de donde venia. Y el delta que se
            // audita es el de CICLO -lo que sube la cuota mensual: dos usuarios
            // facturables mas a 15.000-, no el prorrateo del periodo en curso: es el
            // importe que el cliente vio antes de aceptar.
            // El id de la sucesora va como any() a proposito: la fila todavia no
            // existe cuando se construye, y el repositorio simulado devuelve el
            // argumento sin asignarle uno.
            ArgumentCaptor<BigDecimal> delta = ArgumentCaptor.forClass(BigDecimal.class);
            verify(audit).itemQuantityChanged(eq(CONTRATO), any(), eq(5), eq(7), delta.capture(),
                    eq(902L));
            assertThat(delta.getValue()).isEqualByComparingTo("30000.00");
        }

        @Test
        @DisplayName("anuncia el cambio para que se recalculen los permisos")
        void anunciaElCambio() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            service.execute(comando(7));

            verify(subscriptionChangedPort).subscriptionChanged(new SubscriptionChangedEvent(
                    EMPRESA, CONTRATO, SubscriptionChangeKind.QUANTITY_CHANGED, ENERO_17));
        }

        @Test
        @DisplayName("los dos tramos no dejan hueco ni se pisan el dia del cambio")
        void sinHuecoNiSolape() {
            escenarioFeliz();
            when(itemRepository.findOverlapping(anyLong(), anyLong(), anyLong(), any(), any(),
                    any())).thenReturn(List.of());

            service.execute(comando(7));

            ArgumentCaptor<SubscriptionItem> captor = ArgumentCaptor
                    .forClass(SubscriptionItem.class);
            verify(itemRepository, times(2)).save(captor.capture());
            SubscriptionItem original = captor.getAllValues().get(0);
            SubscriptionItem sucesora = captor.getAllValues().get(1);

            assertThat(original.overlaps(sucesora.getPeriod())).isFalse();
            assertThat(original.isCurrentOn(ENERO_17.minusDays(1))).isTrue();
            assertThat(sucesora.isCurrentOn(ENERO_17)).isTrue();
        }
    }

    @Nested
    @DisplayName("Solapes e idempotencia")
    class SolapesEIdempotencia {

        @Test
        @DisplayName("si un tramo futuro se pisa con la sucesora, se rechaza")
        void tramoFuturoQueSePisa() {
            escenarioFeliz();
            SubscriptionItem tramoFuturo = SubscriptionItem.open(EMPRESA, CONTRATO, ARTICULO,
                    "EXTRA_USER", "Usuario adicional", SubscriptionItemType.CAPACITY, "USER", 2,
                    TaxTreatment.TAXED, 3, PRECIO_FIRMADO, BigDecimal.ZERO,
                    new EffectivePeriod(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31)),
                    ItemOrigin.ADDON, null);
            when(itemRepository.findOverlapping(EMPRESA, CONTRATO, ARTICULO, ENERO_17, null, LINEA))
                    .thenReturn(List.of(tramoFuturo));

            assertThatThrownBy(() -> service.execute(comando(7)))
                    .isInstanceOf(SubscriptionItemOverlapException.class);
        }

        @Test
        @DisplayName("el reintento devuelve la sucesora del primer intento")
        void reintentoNoDuplica() {
            SubscriptionItem sucesoraYaCreada = new SubscriptionItem(501L, EMPRESA, CONTRATO,
                    ARTICULO, "EXTRA_USER", "Usuario adicional", SubscriptionItemType.CAPACITY,
                    "USER", 2, TaxTreatment.TAXED, 7, PRECIO_FIRMADO, BigDecimal.ZERO,
                    EffectivePeriod.openFrom(ENERO_17), ItemOrigin.QUANTITY_CHANGE, 902L, null,
                    null, 0L, true);
            when(amendmentRepository.findByClientRequestIdAndCompanyId(LLAVE, EMPRESA))
                    .thenReturn(Optional.of(otrosiGuardado()));
            when(itemRepository.findByCreatedAmendmentIdAndCompanyId(902L, EMPRESA))
                    .thenReturn(Optional.of(sucesoraYaCreada));

            SubscriptionItemDto resultado = service.execute(comando(7));

            assertThat(resultado.id()).isEqualTo(501L);
            verify(itemRepository, never()).save(any());
            verify(amendmentRepository, never()).save(any());
        }
    }
}
