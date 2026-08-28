package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateBillingDocumentService — un periodo exacto, un solo documento")
class GenerateBillingDocumentServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDate AGOSTO_1 = LocalDate.of(2026, 8, 1);
    private static final LocalDate AGOSTO_31 = LocalDate.of(2026, 8, 31);

    @Mock
    private BillingDocumentRepository documentRepository;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private BillingDocumentSequenceRepository sequenceRepository;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;

    @Mock
    private SubscriptionBillingMetrics metrics;
    @Mock
    private SubscriptionBillingAuditPort audit;

    private GenerateBillingDocumentService service;

    @BeforeEach
    void setUp() {
        service = new GenerateBillingDocumentService(documentRepository, chargeRepository,
                sequenceRepository, subscriptionQueryPort, metrics, audit, RELOJ);
    }

    private static SubscriptionCharge cargo(Long id, String subtotal) {
        return new SubscriptionCharge(id, EMPRESA, CONTRATO, null, ChargeType.RECURRING,
                "Plan CORE agosto", new ServicePeriod(AGOSTO_1, AGOSTO_31), BigDecimal.ONE,
                new BigDecimal(subtotal), new BigDecimal(subtotal), new BigDecimal("19.00"),
                TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null, null,
                LocalDateTime.of(2026, 8, 1, 0, 0));
    }

    private GenerateBillingDocumentCommand comando(LocalDate desde, LocalDate hasta,
            BillingReason razon) {
        return new GenerateBillingDocumentCommand(EMPRESA, CONTRATO, razon, desde, hasta);
    }

    private void contratoExiste() {
        when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
    }

    private void devuelveElDocumentoGuardado() {
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionBillingDocument entrante = invocation.getArgument(0);
            return new SubscriptionBillingDocument(900L, entrante.getDocumentNumber(),
                    entrante.getCompanyId(), entrante.getSubscriptionId(),
                    entrante.getDocumentKind(), entrante.getBillingReason(), entrante.getPeriod(),
                    entrante.getIssueStatus(), entrante.getExternal(),
                    entrante.getCorrectsDocumentId(), entrante.getDueDate(),
                    entrante.getSubtotalAmount(), entrante.getTaxAmount(),
                    entrante.getTotalAmount(), entrante.getSettledAmount(), entrante.getTaxes(),
                    entrante.getCreatedDate(), 0L);
        });
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("agrupa los cargos pendientes, numera con la serie DC y sella los cargos")
        void agrupa_numera_y_sella() {
            contratoExiste();
            when(chargeRepository.findPendingByCompanyIdAndSubscription(EMPRESA, CONTRATO, AGOSTO_1,
                    AGOSTO_31)).thenReturn(List.of(cargo(1L, "100000.00"), cargo(2L, "50000.00")));
            when(sequenceRepository.nextNumber("DC")).thenReturn(new DocumentNumber("DC", 1L));
            devuelveElDocumentoGuardado();
            when(chargeRepository.sealAsInvoiced(List.of(1L, 2L), EMPRESA, 900L)).thenReturn(2);

            BillingDocumentDto dto = service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE));

            assertThat(dto.documentNumber()).isEqualTo("DC-000001");
            assertThat(dto.documentKind()).isEqualTo(DocumentKind.INVOICE);
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("150000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("28500.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("178500.00");
            assertThat(dto.dueDate()).isNull();
            assertThat(dto.taxes()).hasSize(1);
            verify(chargeRepository).sealAsInvoiced(List.of(1L, 2L), EMPRESA, 900L);
        }

        @Test
        @DisplayName("sin cargos pendientes no se emite nada, y NO se consume consecutivo")
        void sin_cargos_no_hay_documento() {
            contratoExiste();
            when(chargeRepository.findPendingByCompanyIdAndSubscription(EMPRESA, CONTRATO, AGOSTO_1,
                    AGOSTO_31)).thenReturn(List.of());

            assertThatThrownBy(() -> service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE)))
                    .isInstanceOf(EmptyBillingDocumentException.class);

            verifyNoInteractions(sequenceRepository);
        }

        @Test
        @DisplayName("si no se sellan todos los cargos falla: un documento cuyo subtotal no"
                + " coincide con sus cargos es el descuadre que la conciliacion caza un mes tarde")
        void falla_si_no_se_sellan_todos() {
            contratoExiste();
            when(chargeRepository.findPendingByCompanyIdAndSubscription(EMPRESA, CONTRATO, AGOSTO_1,
                    AGOSTO_31)).thenReturn(List.of(cargo(1L, "100000.00"), cargo(2L, "50000.00")));
            when(sequenceRepository.nextNumber("DC")).thenReturn(new DocumentNumber("DC", 1L));
            devuelveElDocumentoGuardado();
            when(chargeRepository.sealAsInvoiced(anyList(), anyLong(), anyLong())).thenReturn(1);

            assertThatThrownBy(() -> service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE)))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("but sealed 1");
        }
    }

    @Nested
    @DisplayName("Barandilla por periodo exacto — TRAMPA 3")
    class BarandillaPorPeriodoExacto {

        @Test
        @DisplayName("regenerar la factura del MISMO periodo exacto se rechaza,"
                + " y sin gastar consecutivo: por eso la serie no deja huecos")
        void no_se_factura_dos_veces_el_mismo_periodo() {
            contratoExiste();
            when(documentRepository.existsRecurringCycle(EMPRESA, CONTRATO, AGOSTO_1, AGOSTO_31))
                    .thenReturn(true);

            assertThatThrownBy(() -> service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE)))
                    .isInstanceOf(DuplicateBillingCycleException.class)
                    .hasMessageContaining("2026-08-01").hasMessageContaining("2026-08-31");

            verifyNoInteractions(sequenceRepository);
            verify(documentRepository, never()).save(any());
            verify(chargeRepository, never()).sealAsInvoiced(anyList(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("la factura ANUAL emitida a mitad de agosto NO choca con la mensual del"
                + " dia 1: se agrupa por periodo exacto, no por mes")
        void el_plan_anual_convive_con_el_mensual() {
            LocalDate anualDesde = LocalDate.of(2026, 8, 15);
            LocalDate anualHasta = LocalDate.of(2027, 8, 14);
            contratoExiste();
            // La mensual de agosto ya existe. La consulta pregunta por los extremos
            // EXACTOS del periodo anual, que son otros, asi que no encuentra nada.
            when(documentRepository.existsRecurringCycle(EMPRESA, CONTRATO, anualDesde, anualHasta))
                    .thenReturn(false);
            when(chargeRepository.findPendingByCompanyIdAndSubscription(EMPRESA, CONTRATO,
                    anualDesde, anualHasta)).thenReturn(List.of(cargo(3L, "1900000.00")));
            when(sequenceRepository.nextNumber("DC")).thenReturn(new DocumentNumber("DC", 2L));
            devuelveElDocumentoGuardado();
            when(chargeRepository.sealAsInvoiced(List.of(3L), EMPRESA, 900L)).thenReturn(1);

            BillingDocumentDto dto = service
                    .execute(comando(anualDesde, anualHasta, BillingReason.RECURRING_CYCLE));

            assertThat(dto.periodStart()).isEqualTo(anualDesde);
            assertThat(dto.periodEnd()).isEqualTo(anualHasta);
            verify(documentRepository).existsRecurringCycle(EMPRESA, CONTRATO, anualDesde,
                    anualHasta);
        }

        @Test
        @DisplayName("un cobro de prorrateo del mismo periodo exacto NO se bloquea:"
                + " la barandilla solo mira las facturas de ciclo")
        void el_prorrateo_no_entra_en_la_barandilla() {
            contratoExiste();
            when(chargeRepository.findPendingByCompanyIdAndSubscription(EMPRESA, CONTRATO, AGOSTO_1,
                    AGOSTO_31)).thenReturn(List.of(cargo(4L, "34838.71")));
            when(sequenceRepository.nextNumber("DC")).thenReturn(new DocumentNumber("DC", 3L));
            devuelveElDocumentoGuardado();
            when(chargeRepository.sealAsInvoiced(List.of(4L), EMPRESA, 900L)).thenReturn(1);

            BillingDocumentDto dto = service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.PRORATION));

            assertThat(dto.billingReason()).isEqualTo(BillingReason.PRORATION);
            verify(documentRepository, never()).existsRecurringCycle(anyLong(), anyLong(), any(),
                    any());
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un contrato que no es de la empresa no existe para este caso de uso,"
                + " y no se toca ni el consecutivo ni los cargos")
        void el_contrato_ajeno_no_existe() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found");

            verifyNoInteractions(sequenceRepository, chargeRepository);
            verify(documentRepository, never()).save(any());
        }

        @Test
        @DisplayName("el contrato se resuelve SIEMPRE por la variante acotada por empresa")
        void siempre_por_la_variante_acotada() {
            contratoExiste();
            when(documentRepository.existsRecurringCycle(anyLong(), anyLong(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.RECURRING_CYCLE)))
                    .isInstanceOf(DuplicateBillingCycleException.class);

            verify(subscriptionQueryPort).findByIdAndCompanyId(eq(CONTRATO), eq(EMPRESA));
        }
    }

    @Nested
    @DisplayName("Consecutivo")
    class Consecutivo {

        @Test
        @DisplayName("pide el numero por la serie del tipo de documento, nunca un maximo mas uno")
        void numera_por_la_serie() {
            contratoExiste();
            when(chargeRepository.findPendingByCompanyIdAndSubscription(anyLong(), anyLong(), any(),
                    any())).thenReturn(List.of(cargo(1L, "100000.00")));
            when(sequenceRepository.nextNumber(anyString()))
                    .thenReturn(new DocumentNumber("DC", 17L));
            devuelveElDocumentoGuardado();
            when(chargeRepository.sealAsInvoiced(anyList(), anyLong(), anyLong())).thenReturn(1);

            BillingDocumentDto dto = service
                    .execute(comando(AGOSTO_1, AGOSTO_31, BillingReason.ONE_TIME));

            assertThat(dto.documentNumber()).isEqualTo("DC-000017");
            verify(sequenceRepository).nextNumber("DC");
        }
    }
}
