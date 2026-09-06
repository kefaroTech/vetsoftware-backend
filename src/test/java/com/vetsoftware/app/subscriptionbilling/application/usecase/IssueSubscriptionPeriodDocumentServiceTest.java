package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillableSubscriptionItemPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

/**
 * El caso de uso extraido de
 * {@code RunSubscriptionBillingCycleService.billOne}: el mismo {@code accrue()}
 * + {@code issue()} de siempre, para UN contrato y UN periodo, sin avanzar
 * ningun calendario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IssueSubscriptionPeriodDocumentService — el documento de un periodo exacto")
class IssueSubscriptionPeriodDocumentServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate PERIOD_START = LocalDate.of(2026, 3, 2);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 4, 1);
    private static final Clock RELOJ = Clock
            .fixed(PERIOD_START.atTime(4, 40).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final IssueSubscriptionPeriodDocumentCommand COMANDO = new IssueSubscriptionPeriodDocumentCommand(
            EMPRESA, CONTRATO, PERIOD_START, PERIOD_END);

    @Mock
    private BillableSubscriptionItemPort itemPort;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private GenerateBillingDocumentUseCase generateUseCase;
    @Mock
    private BillingDocumentRepository documentRepository;

    private IssueSubscriptionPeriodDocumentService service;

    @BeforeEach
    void setUp() {
        service = new IssueSubscriptionPeriodDocumentService(itemPort, chargeRepository,
                generateUseCase, documentRepository, RELOJ);
    }

    // ------------------------------------------------------------------ fixtures

    private static BillableSubscriptionItem linea(Long id, ItemChargeMode modo, int cantidad,
            int incluido, String tarifa) {
        return new BillableSubscriptionItem(id, EMPRESA, CONTRATO, 500L, "Modulo agenda", modo,
                cantidad, incluido, new BigDecimal(tarifa), new BigDecimal("19.00"),
                TaxTreatment.TAXED, LocalDate.of(2026, 1, 1), null);
    }

    private static BillingDocumentDto documentoEmitido(Long id, String numero, BigDecimal total) {
        return new BillingDocumentDto(id, numero, EMPRESA, CONTRATO, DocumentKind.INVOICE,
                BillingReason.RECURRING_CYCLE, PERIOD_START, PERIOD_END, IssueStatus.DRAFT, null,
                null, null, null, null, null, null, null,
                total.subtract(new BigDecimal("19000.00")), new BigDecimal("19000.00"), total,
                BigDecimal.ZERO, total, List.of(), LocalDateTime.of(2026, 3, 2, 4, 40), 0L);
    }

    private static SubscriptionBillingDocument documentoExistente(Long id, String numero,
            BigDecimal total) {
        return new SubscriptionBillingDocument(id, numero, EMPRESA, CONTRATO, DocumentKind.INVOICE,
                BillingReason.RECURRING_CYCLE, new ServicePeriod(PERIOD_START, PERIOD_END),
                IssueStatus.DRAFT, null, null, null, total.subtract(new BigDecimal("19000.00")),
                new BigDecimal("19000.00"), total, BigDecimal.ZERO, List.of(),
                LocalDateTime.of(2026, 3, 2, 4, 40), 0L);
    }

    // ------------------------------------------------------------------ pruebas

    @Nested
    @DisplayName("Los tres desenlaces del periodo")
    class TresDesenlaces {

        @Test
        @DisplayName("emite el documento y devuelve su total")
        void emite_y_devuelve_el_total() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("70210.00")));

            IssuedPeriodDocumentDto resultado = service.execute(COMANDO);

            assertThat(resultado.documentId()).isEqualTo(1L);
            assertThat(resultado.documentNumber()).isEqualTo("DC-2026-0001");
            assertThat(resultado.totalAmount()).isEqualByComparingTo("70210.00");
            assertThat(resultado.currency()).isEqualTo("COP");
            assertThat(resultado.issued()).isTrue();
            assertThat(resultado.accruedCharges()).isEqualTo(1);
        }

        @Test
        @DisplayName("un periodo ya facturado devuelve el documento existente con issued=false")
        void periodo_duplicado_devuelve_el_existente() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START)).thenReturn(List.of());
            when(generateUseCase.execute(any())).thenThrow(
                    new DuplicateBillingCycleException(CONTRATO, PERIOD_START, PERIOD_END));
            when(documentRepository.findRecurringCycleDocument(EMPRESA, CONTRATO, PERIOD_START,
                    PERIOD_END))
                    .thenReturn(Optional.of(
                            documentoExistente(3L, "DC-2026-0003", new BigDecimal("70210.00"))));

            IssuedPeriodDocumentDto resultado = service.execute(COMANDO);

            assertThat(resultado.documentId()).isEqualTo(3L);
            assertThat(resultado.documentNumber()).isEqualTo("DC-2026-0003");
            assertThat(resultado.totalAmount()).isEqualByComparingTo("70210.00");
            assertThat(resultado.issued()).isFalse();
        }

        @Test
        @DisplayName("un periodo sin cargos pendientes devuelve issued=false sin documento")
        void periodo_vacio_no_emite_nada() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START)).thenReturn(List.of());
            when(generateUseCase.execute(any()))
                    .thenThrow(new EmptyBillingDocumentException(CONTRATO));

            IssuedPeriodDocumentDto resultado = service.execute(COMANDO);

            assertThat(resultado.documentId()).isNull();
            assertThat(resultado.documentNumber()).isNull();
            assertThat(resultado.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resultado.currency()).isEqualTo("COP");
            assertThat(resultado.issued()).isFalse();
        }

        @Test
        @DisplayName("no calcula ni avanza ningun periodo siguiente: emite exactamente el que"
                + " llego en el comando")
        void no_avanza_el_periodo() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START)).thenReturn(List.of());
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", BigDecimal.ZERO));

            service.execute(COMANDO);

            ArgumentCaptor<GenerateBillingDocumentCommand> comando = ArgumentCaptor
                    .forClass(GenerateBillingDocumentCommand.class);
            verify(generateUseCase).execute(comando.capture());
            assertThat(comando.getValue().periodStart()).isEqualTo(PERIOD_START);
            assertThat(comando.getValue().periodEnd()).isEqualTo(PERIOD_END);
        }
    }

    @Nested
    @DisplayName("Se factura por linea en modo de pago, nunca por estado del contrato")
    class PorLineaNoPorContrato {

        @Test
        @DisplayName("una linea PAID si devenga")
        void una_linea_paid_devenga() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.TRIAL, 1, 0, "179000.00"),
                            linea(901L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("70210.00")));

            IssuedPeriodDocumentDto resultado = service.execute(COMANDO);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getSubscriptionItemId()).isEqualTo(901L);
            assertThat(cargo.getValue().getSubtotalAmount()).isEqualByComparingTo("59000.00");
            assertThat(resultado.accruedCharges()).isEqualTo(1);
        }

        /**
         * R-TRIAL-14: la linea gratuita <b>conserva su tarifa real</b>, asi que
         * olvidarse del modo de cobro no produce ceros — produce la tarifa completa
         * cobrada a todos los clientes en prueba.
         */
        @Test
        @DisplayName("una linea TRIAL no devenga aunque lleve tarifa completa guardada")
        void una_linea_trial_no_devenga() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.TRIAL, 1, 0, "179000.00")));
            when(generateUseCase.execute(any()))
                    .thenThrow(new EmptyBillingDocumentException(CONTRATO));

            service.execute(COMANDO);

            verify(chargeRepository, never()).save(any());
        }

        @Test
        @DisplayName("una linea consumida entera dentro de lo incluido no genera cargo")
        void todo_incluido_no_genera_cargo() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 3, 3, "59000.00")));
            when(generateUseCase.execute(any()))
                    .thenThrow(new EmptyBillingDocumentException(CONTRATO));

            service.execute(COMANDO);

            verify(chargeRepository, never()).save(any());
        }

        @Test
        @DisplayName("cobra solo lo que pasa de lo incluido")
        void cobra_solo_el_exceso() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 5, 3, "10000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("23800.00")));

            service.execute(COMANDO);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getQuantity()).isEqualByComparingTo("2");
            assertThat(cargo.getValue().getSubtotalAmount()).isEqualByComparingTo("20000.00");
        }
    }

    @Nested
    @DisplayName("Un reinicio a mitad no duplica")
    class ReinicioIdempotente {

        @Test
        @DisplayName("la llave se calcula con la LINEA, no con el articulo")
        void la_llave_lleva_la_linea() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("70210.00")));

            service.execute(COMANDO);

            ArgumentCaptor<RecurringChargeKey> llave = ArgumentCaptor
                    .forClass(RecurringChargeKey.class);
            verify(chargeRepository).existsRecurringCharge(llave.capture());
            assertThat(llave.getValue()).isEqualTo(
                    new RecurringChargeKey(EMPRESA, CONTRATO, 900L, PERIOD_START, PERIOD_END));
        }

        @Test
        @DisplayName("no vuelve a devengar la linea que ya tiene su cargo de este periodo")
        void no_devenga_dos_veces() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(true);
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("70210.00")));

            IssuedPeriodDocumentDto resultado = service.execute(COMANDO);

            verify(chargeRepository, never()).save(any());
            assertThat(resultado.accruedCharges()).isZero();
        }
    }

    @Nested
    @DisplayName("El reloj lo pone la inyeccion, no el llamador")
    class RelojInyectado {

        @Test
        @DisplayName("el cargo devengado nace con la fecha del reloj inyectado")
        void el_cargo_usa_el_reloj() {
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, PERIOD_START))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(generateUseCase.execute(any()))
                    .thenReturn(documentoEmitido(1L, "DC-2026-0001", new BigDecimal("70210.00")));

            service.execute(COMANDO);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getCreatedDate())
                    .isEqualTo(LocalDateTime.of(2026, 3, 2, 4, 40));
        }
    }
}
