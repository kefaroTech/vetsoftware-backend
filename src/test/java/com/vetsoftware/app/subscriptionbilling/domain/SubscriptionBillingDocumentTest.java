package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SubscriptionBillingDocument — la cuenta de cobro, que no cambia de importe")
class SubscriptionBillingDocumentTest {

    /** El documento se calcula el 1 de agosto. */
    private static final Clock RELOJ_DE_CALCULO = Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"),
            ZoneId.of("America/Bogota"));
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 8, 1, 7, 0);
    private static final ServicePeriod AGOSTO = new ServicePeriod(LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31));
    private static final Long EMPRESA = 42L;

    private static SubscriptionCharge cuota(String subtotal) {
        return new SubscriptionCharge(500L, EMPRESA, 7L, null, ChargeType.RECURRING, "Plan CORE",
                AGOSTO, BigDecimal.ONE, new BigDecimal(subtotal).abs(), new BigDecimal(subtotal),
                new BigDecimal("19.00"), TaxTreatment.TAXED, null, ChargeStatus.PENDING, null, null,
                null, AHORA);
    }

    private static SubscriptionBillingDocument factura() {
        TaxBreakdown breakdown = TaxBreakdown.of(List.of(cuota("100000.00")), DocumentKind.INVOICE,
                EMPRESA, AHORA);
        return new SubscriptionBillingDocument(900L, "DC-000001", EMPRESA, 7L, DocumentKind.INVOICE,
                BillingReason.RECURRING_CYCLE, AGOSTO, IssueStatus.DRAFT, null, null, null,
                breakdown.subtotalAmount(), breakdown.taxAmount(), breakdown.totalAmount(),
                new BigDecimal("0.00"), breakdown.lineas(), AHORA, 0L);
    }

    private static ExternalInvoiceReference referencia(LocalDate fechaFiscal) {
        return new ExternalInvoiceReference("FE-4711", "CUFE123", fechaFiscal, "SIIGO",
                LocalDateTime.of(2026, 8, 20, 9, 0), 3L);
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("nace DRAFT, sin referencia externa, SIN vencimiento y con cero saldado")
        void nace_en_borrador() {
            TaxBreakdown breakdown = TaxBreakdown.of(List.of(cuota("100000.00")),
                    DocumentKind.INVOICE, EMPRESA, AHORA);

            SubscriptionBillingDocument document = SubscriptionBillingDocument.issue(
                    new DocumentNumber("DC", 1L), EMPRESA, 7L, DocumentKind.INVOICE,
                    BillingReason.RECURRING_CYCLE, AGOSTO, breakdown, null, RELOJ_DE_CALCULO);

            assertThat(document.getDocumentNumber()).isEqualTo("DC-000001");
            assertThat(document.getIssueStatus()).isEqualTo(IssueStatus.DRAFT);
            assertThat(document.getExternal()).isNull();
            assertThat(document.getDueDate()).isNull();
            assertThat(document.getSettledAmount()).isEqualByComparingTo("0.00");
            assertThat(document.getTotalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("total = subtotal + impuesto, y ningun importe es negativo")
        void importes_coherentes_y_positivos() {
            assertThatThrownBy(() -> new SubscriptionBillingDocument(null, "DC-000001", EMPRESA, 7L,
                    DocumentKind.INVOICE, BillingReason.RECURRING_CYCLE, AGOSTO, IssueStatus.DRAFT,
                    null, null, null, new BigDecimal("100.00"), new BigDecimal("19.00"),
                    new BigDecimal("200.00"), BigDecimal.ZERO, List.of(), AHORA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subtotalAmount plus taxAmount");

            assertThatThrownBy(() -> new SubscriptionBillingDocument(null, "DC-000001", EMPRESA, 7L,
                    DocumentKind.CREDIT_NOTE, BillingReason.ADJUSTMENT, AGOSTO, IssueStatus.DRAFT,
                    null, null, null, new BigDecimal("-100.00"), BigDecimal.ZERO,
                    new BigDecimal("-100.00"), BigDecimal.ZERO, List.of(), AHORA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("always positive");
        }

        @Test
        @DisplayName("solo una nota credito o debito puede corregir otro documento")
        void solo_las_notas_corrigen() {
            assertThatThrownBy(() -> new SubscriptionBillingDocument(null, "DC-000002", EMPRESA, 7L,
                    DocumentKind.INVOICE, BillingReason.ADJUSTMENT, AGOSTO, IssueStatus.DRAFT, null,
                    900L, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    List.of(), AHORA, null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("credit or debit note");
        }

        @Test
        @DisplayName("un documento no se corrige a si mismo: el CHECK no puede decirlo"
                + " porque el id es AUTO_INCREMENT, asi que lo dice el dominio")
        void no_se_corrige_a_si_mismo() {
            assertThatThrownBy(() -> new SubscriptionBillingDocument(900L, "NC-000001", EMPRESA, 7L,
                    DocumentKind.CREDIT_NOTE, BillingReason.ADJUSTMENT, AGOSTO, IssueStatus.DRAFT,
                    null, 900L, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, List.of(), AHORA, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot correct itself");
        }
    }

    @Nested
    @DisplayName("Saldo calculado — TRAMPA 2")
    class SaldoCalculado {

        @Test
        @DisplayName("el saldo se deriva de total - saldado, con la misma formula que la columna"
                + " generada de la base")
        void el_saldo_se_deriva() {
            SubscriptionBillingDocument document = factura();

            assertThat(document.getBalanceAmount()).isEqualByComparingTo("119000.00");
            document.settle(new BigDecimal("19000.00"));
            assertThat(document.getBalanceAmount()).isEqualByComparingTo("100000.00");
        }

        @Test
        @DisplayName("el dominio no tiene ningun campo de saldo: no hay estado que desincronizar")
        void el_dominio_no_guarda_el_saldo() {
            List<String> campos = Arrays
                    .stream(SubscriptionBillingDocument.class.getDeclaredFields())
                    .map(Field::getName).toList();

            assertThat(campos).contains("totalAmount", "settledAmount")
                    .doesNotContain("balanceAmount");
        }

        @Test
        @DisplayName("el dominio no expone ningun mutador de importe: solo `settle`,"
                + " que valida el tope contra el total antes de mover nada")
        void no_existe_mutador_de_importe() {
            List<String> metodosDelDominio = Arrays
                    .stream(SubscriptionBillingDocument.class.getDeclaredMethods())
                    .map(m -> m.getName()).toList();

            assertThat(metodosDelDominio).doesNotContain("setBalanceAmount", "setTotalAmount",
                    "setSubtotalAmount", "setTaxAmount", "setSettledAmount", "setPeriod",
                    "setDocumentKind");
            assertThat(metodosDelDominio).contains("settle", "getBalanceAmount");
        }

        @Test
        @DisplayName("saldar mas de lo que se debe se rechaza: es como la cartera acaba"
                + " cuadrando con plata que no existe")
        void no_se_salda_de_mas() {
            SubscriptionBillingDocument document = factura();

            assertThatThrownBy(() -> document.settle(new BigDecimal("119000.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds the document total");
        }
    }

    @Nested
    @DisplayName("Vencimiento — TRAMPA 4")
    class Vencimiento {

        @Test
        @DisplayName("se cuenta desde external_issued_at, la fecha fiscal, NUNCA desde"
                + " la fecha en que se calculo el cobro aqui")
        void se_cuenta_desde_la_fecha_fiscal() {
            SubscriptionBillingDocument document = factura();
            // Calculado el 1 de agosto; emitido fuera el 20. Con 15 dias de plazo,
            // contarlo desde el calculo daria el 16 de agosto -ya vencida al
            // registrarla- y suspenderia la cuenta por un retraso administrativo propio.
            document.registerExternalInvoice(referencia(LocalDate.of(2026, 8, 20)), 15);

            assertThat(document.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 4));
            assertThat(document.getDueDate())
                    .isNotEqualTo(document.getCreatedDate().toLocalDate().plusDays(15));
        }

        @Test
        @DisplayName("mientras no hay factura externa no hay vencimiento")
        void sin_factura_externa_no_hay_vencimiento() {
            SubscriptionBillingDocument document = factura();
            document.submitForExternalIssue();

            assertThat(document.getIssueStatus()).isEqualTo(IssueStatus.AWAITING_EXTERNAL);
            assertThat(document.getDueDate()).isNull();
        }

        @Test
        @DisplayName("un plazo de cero dias vence el mismo dia fiscal, no antes")
        void plazo_cero() {
            SubscriptionBillingDocument document = factura();
            document.registerExternalInvoice(referencia(LocalDate.of(2026, 8, 20)), 0);

            assertThat(document.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        }

        @Test
        @DisplayName("un vencimiento anterior a la fecha fiscal no se puede construir")
        void nunca_antes_de_la_fecha_fiscal() {
            assertThatThrownBy(() -> new SubscriptionBillingDocument(900L, "DC-000001", EMPRESA, 7L,
                    DocumentKind.INVOICE, BillingReason.RECURRING_CYCLE, AGOSTO,
                    IssueStatus.EXTERNAL_REGISTERED, referencia(LocalDate.of(2026, 8, 20)), null,
                    LocalDate.of(2026, 8, 1), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, List.of(), AHORA, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("before the external issue date");
        }
    }

    @Nested
    @DisplayName("La factura externa sella el importe — TRAMPA 5")
    class FacturaExternaSella {

        @Test
        @DisplayName("no hay ningun mutador de importe, periodo ni tipo: son campos final")
        void los_importes_son_final() {
            List<String> mutables = Arrays
                    .stream(SubscriptionBillingDocument.class.getDeclaredFields())
                    .filter(f -> !f.isSynthetic()).filter(f -> !Modifier.isStatic(f.getModifiers()))
                    .filter(f -> !Modifier.isFinal(f.getModifiers())).map(Field::getName).toList();

            assertThat(mutables).containsExactlyInAnyOrder("issueStatus", "external", "dueDate",
                    "settledAmount", "version");
        }

        @Test
        @DisplayName("registrar una segunda factura externa se rechaza y remite a la nota credito")
        void no_se_registra_dos_veces() {
            SubscriptionBillingDocument document = factura();
            document.registerExternalInvoice(referencia(LocalDate.of(2026, 8, 20)), 15);

            assertThatThrownBy(() -> document
                    .registerExternalInvoice(referencia(LocalDate.of(2026, 9, 1)), 15))
                    .isInstanceOf(BillingDocumentAlreadyIssuedException.class)
                    .hasMessageContaining("credit note");
            assertThat(document.getExternal().invoiceNumber()).isEqualTo("FE-4711");
            assertThat(document.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 4));
        }

        @Test
        @DisplayName("un documento con factura externa registrada NO se anula aqui:"
                + " la DIAN ya tiene la factura")
        void no_se_anula_lo_ya_emitido() {
            SubscriptionBillingDocument document = factura();
            document.registerExternalInvoice(referencia(LocalDate.of(2026, 8, 20)), 15);

            assertThatThrownBy(document::voidDocument)
                    .isInstanceOf(BillingDocumentAlreadyIssuedException.class);
            assertThat(document.getIssueStatus()).isEqualTo(IssueStatus.EXTERNAL_REGISTERED);
        }

        @Test
        @DisplayName("lo que si cambia despues de emitir es lo saldado, y con ello el saldo")
        void lo_unico_que_se_mueve_es_lo_saldado() {
            SubscriptionBillingDocument document = factura();
            document.registerExternalInvoice(referencia(LocalDate.of(2026, 8, 20)), 15);

            document.settle(new BigDecimal("119000.00"));

            assertThat(document.getSettledAmount()).isEqualByComparingTo("119000.00");
            assertThat(document.getBalanceAmount()).isEqualByComparingTo("0.00");
            assertThat(document.getTotalAmount()).isEqualByComparingTo("119000.00");
        }

        @Test
        @DisplayName("un borrador si se anula, y anularlo libera el periodo para reemitirlo")
        void el_borrador_se_anula() {
            SubscriptionBillingDocument document = factura();

            document.voidDocument();

            assertThat(document.getIssueStatus()).isEqualTo(IssueStatus.VOIDED);
            assertThat(document.entraEnLaBarandillaDeCiclo()).isFalse();
            assertThatThrownBy(document::voidDocument)
                    .isInstanceOf(BillingDocumentAlreadyVoidedException.class);
        }
    }

    @Nested
    @DisplayName("Barandilla del ciclo — TRAMPA 3, lado del dominio")
    class Barandilla {

        @Test
        @DisplayName("solo entra la factura de ciclo no anulada: es lo que replica"
                + " el CASE de recurring_cycle_marker")
        void solo_la_factura_de_ciclo_viva() {
            assertThat(factura().entraEnLaBarandillaDeCiclo()).isTrue();
            assertThat(conRazon(BillingReason.PRORATION).entraEnLaBarandillaDeCiclo()).isFalse();
            assertThat(conRazon(BillingReason.ONE_TIME).entraEnLaBarandillaDeCiclo()).isFalse();
        }

        private SubscriptionBillingDocument conRazon(BillingReason reason) {
            SubscriptionBillingDocument base = factura();
            return new SubscriptionBillingDocument(base.getId(), base.getDocumentNumber(),
                    base.getCompanyId(), base.getSubscriptionId(), base.getDocumentKind(), reason,
                    base.getPeriod(), base.getIssueStatus(), null, null, null,
                    base.getSubtotalAmount(), base.getTaxAmount(), base.getTotalAmount(),
                    base.getSettledAmount(), base.getTaxes(), base.getCreatedDate(),
                    base.getVersion());
        }
    }
}
