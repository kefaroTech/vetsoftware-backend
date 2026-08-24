package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillingDocumentSequenceRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.subscriptionbilling.domain.ExternalInvoiceReference;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionbilling.domain.MixedSignChargesException;
import com.vetsoftware.app.subscriptionbilling.domain.ServicePeriod;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocument;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionBillingDocumentNotFoundException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
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
@DisplayName("IssueCreditNoteService — corregir un documento emitido es emitir otro")
class IssueCreditNoteServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));

    @Mock
    private BillingDocumentRepository documentRepository;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private BillingDocumentSequenceRepository sequenceRepository;

    private IssueCreditNoteService service;

    @BeforeEach
    void setUp() {
        service = new IssueCreditNoteService(documentRepository, chargeRepository,
                sequenceRepository, RELOJ);
    }

    private static SubscriptionBillingDocument facturaRegistrada() {
        return new SubscriptionBillingDocument(900L, "DC-000001", EMPRESA, 7L, DocumentKind.INVOICE,
                BillingReason.RECURRING_CYCLE, AGOSTO, IssueStatus.EXTERNAL_REGISTERED,
                new ExternalInvoiceReference("FE-4711", "CUFE123", LocalDate.of(2026, 8, 20),
                        "SIIGO", LocalDateTime.of(2026, 8, 21, 9, 0), 3L),
                null, LocalDate.of(2026, 9, 4), new BigDecimal("100000.00"),
                new BigDecimal("19000.00"), new BigDecimal("119000.00"), BigDecimal.ZERO, List.of(),
                LocalDateTime.of(2026, 8, 1, 7, 0), 0L);
    }

    private static SubscriptionCharge credito(Long id, String subtotal) {
        return new SubscriptionCharge(id, EMPRESA, 7L, null, ChargeType.CREDIT, "Devolucion",
                AGOSTO, BigDecimal.ONE, new BigDecimal(subtotal).abs(), new BigDecimal(subtotal),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null,
                null, LocalDateTime.of(2026, 9, 25, 7, 0));
    }

    private static SubscriptionCharge cuota(Long id, String subtotal) {
        return new SubscriptionCharge(id, EMPRESA, 7L, null, ChargeType.RECURRING, "Cuota", AGOSTO,
                BigDecimal.ONE, new BigDecimal(subtotal), new BigDecimal(subtotal),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null,
                null, LocalDateTime.of(2026, 9, 25, 7, 0));
    }

    private void devuelveLaNotaGuardada() {
        when(documentRepository.save(any())).thenAnswer(invocation -> {
            SubscriptionBillingDocument entrante = invocation.getArgument(0);
            return new SubscriptionBillingDocument(901L, entrante.getDocumentNumber(),
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
    @DisplayName("Encadenamiento — TRAMPA 5")
    class Encadenamiento {

        @Test
        @DisplayName("la nota queda encadenada al original por correctsDocumentId,"
                + " numerada con su propia serie NC, y el original NO se toca")
        void encadena_y_no_toca_el_original() {
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(facturaRegistrada()));
            when(chargeRepository.findAllByIdsAndCompanyId(List.of(11L), EMPRESA))
                    .thenReturn(List.of(credito(11L, "-100000.00")));
            when(sequenceRepository.nextNumber("NC")).thenReturn(new DocumentNumber("NC", 1L));
            devuelveLaNotaGuardada();
            when(chargeRepository.sealAsInvoiced(List.of(11L), EMPRESA, 901L)).thenReturn(1);

            BillingDocumentDto dto = service
                    .execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L)));

            assertThat(dto.documentNumber()).isEqualTo("NC-000001");
            assertThat(dto.documentKind()).isEqualTo(DocumentKind.CREDIT_NOTE);
            assertThat(dto.billingReason()).isEqualTo(BillingReason.ADJUSTMENT);
            assertThat(dto.correctsDocumentId()).isEqualTo(900L);
            assertThat(dto.issueStatus()).isEqualTo(IssueStatus.DRAFT);
            // Importes SIEMPRE positivos: el signo lo da el document_kind.
            assertThat(dto.subtotalAmount()).isEqualByComparingTo("100000.00");
            assertThat(dto.taxAmount()).isEqualByComparingTo("19000.00");
            assertThat(dto.totalAmount()).isEqualByComparingTo("119000.00");
            // El documento corregido se leyo, y no se guardo.
            verify(documentRepository).findByIdAndCompanyId(900L, EMPRESA);
            verify(documentRepository).save(any());
        }

        @Test
        @DisplayName("hereda el periodo del documento que corrige: la nota declara"
                + " el mismo periodo fiscal que la factura")
        void hereda_el_periodo() {
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(facturaRegistrada()));
            when(chargeRepository.findAllByIdsAndCompanyId(List.of(11L), EMPRESA))
                    .thenReturn(List.of(credito(11L, "-100000.00")));
            when(sequenceRepository.nextNumber("NC")).thenReturn(new DocumentNumber("NC", 1L));
            devuelveLaNotaGuardada();
            when(chargeRepository.sealAsInvoiced(anyList(), anyLong(), anyLong())).thenReturn(1);

            BillingDocumentDto dto = service
                    .execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L)));

            assertThat(dto.periodStart()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(dto.periodEnd()).isEqualTo(LocalDate.of(2026, 8, 31));
        }
    }

    @Nested
    @DisplayName("Convencion de signos — TRAMPA 1")
    class ConvencionDeSignos {

        @Test
        @DisplayName("una nota credito con cargos de los dos signos se rechaza, y no gasta"
                + " consecutivo")
        void no_se_mezclan_signos() {
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(facturaRegistrada()));
            when(chargeRepository.findAllByIdsAndCompanyId(List.of(11L, 12L), EMPRESA))
                    .thenReturn(List.of(credito(11L, "-100000.00"), cuota(12L, "40000.00")));

            assertThatThrownBy(() -> service
                    .execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L, 12L))))
                    .isInstanceOf(MixedSignChargesException.class);

            verifyNoInteractions(sequenceRepository);
            verify(documentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un cargo ya facturado no se acredita otra vez")
        void no_se_acredita_lo_ya_facturado() {
            SubscriptionCharge yaFacturado = new SubscriptionCharge(11L, EMPRESA, 7L, null,
                    ChargeType.CREDIT, "Devolucion", AGOSTO, BigDecimal.ONE,
                    new BigDecimal("100000.00"), new BigDecimal("-100000.00"),
                    new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.INVOICED, null,
                    800L, null, LocalDateTime.of(2026, 9, 25, 7, 0));
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(facturaRegistrada()));
            when(chargeRepository.findAllByIdsAndCompanyId(List.of(11L), EMPRESA))
                    .thenReturn(List.of(yaFacturado));

            assertThatThrownBy(
                    () -> service.execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be credited again");
        }

        @Test
        @DisplayName("un cargo que no es de la empresa no aparece, y la cuenta no cuadra")
        void cargo_de_otra_empresa_no_aparece() {
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.of(facturaRegistrada()));
            when(chargeRepository.findAllByIdsAndCompanyId(List.of(11L, 99L), EMPRESA))
                    .thenReturn(List.of(credito(11L, "-100000.00")));

            assertThatThrownBy(() -> service
                    .execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L, 99L))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("do not exist in company");
        }

        @Test
        @DisplayName("el documento a corregir se resuelve acotado por empresa")
        void documento_ajeno_no_existe() {
            when(documentRepository.findByIdAndCompanyId(900L, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new IssueCreditNoteCommand(EMPRESA, 900L, List.of(11L))))
                    .isInstanceOf(SubscriptionBillingDocumentNotFoundException.class);

            verifyNoInteractions(sequenceRepository, chargeRepository);
        }
    }
}
