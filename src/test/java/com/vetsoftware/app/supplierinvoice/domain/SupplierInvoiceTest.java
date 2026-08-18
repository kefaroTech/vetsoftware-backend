package com.vetsoftware.app.supplierinvoice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests del agregado {@link SupplierInvoice}: matemática de dinero
 * (total/neto/saldo con retención), transición de estado por abonos (PENDING →
 * PARTIAL → PAID), rechazo de sobrepago / pago sobre anulada, y anulación solo
 * sin abonos.
 */
class SupplierInvoiceTest {

    private static final CompanyRef CO = new CompanyRef(1L, "Vet SAS", "900123456-7");
    private static final BranchRef BR = new BranchRef(10L, "Principal");
    private static final SupplierRef SUP = new SupplierRef(5L, "Distribuidora", "800111222-3");

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private SupplierInvoice invoice(String subtotal, String tax, String withholding) {
        return SupplierInvoice.create(CO, BR, SUP, null, null, "FV-1", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), bd(subtotal), bd(tax), bd(withholding), null, 7L);
    }

    private SupplierInvoicePayment payment(String amount) {
        return SupplierInvoicePayment.create(bd(amount), LocalDate.of(2026, 7, 10),
                SupplierInvoicePaymentMethod.CASH, null, null, 7L);
    }

    @Test
    void nace_pending_con_totales_derivados_y_saldo_neto() {
        SupplierInvoice inv = invoice("1000", "190", "10");
        assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PENDING);
        assertThat(inv.getTotal()).isEqualByComparingTo("1190"); // base + impuesto
        assertThat(inv.payableAmount()).isEqualByComparingTo("1180"); // total - retención
        assertThat(inv.paidAmount()).isEqualByComparingTo("0");
        assertThat(inv.balance()).isEqualByComparingTo("1180");
    }

    @Test
    void abono_parcial_pasa_a_partial_y_reduce_saldo() {
        SupplierInvoice inv = invoice("1000", "190", "10");
        inv.registerPayment(payment("180"), 7L, null);
        assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PARTIAL);
        assertThat(inv.paidAmount()).isEqualByComparingTo("180");
        assertThat(inv.balance()).isEqualByComparingTo("1000");
    }

    @Test
    void abono_total_pasa_a_paid_y_saldo_cero() {
        SupplierInvoice inv = invoice("1000", "190", "10");
        inv.registerPayment(payment("1180"), 7L, null);
        assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.PAID);
        assertThat(inv.balance()).isEqualByComparingTo("0");
    }

    @Test
    void sobrepago_es_rechazado() {
        SupplierInvoice inv = invoice("1000", "190", "10");
        assertThatThrownBy(() -> inv.registerPayment(payment("1181"), 7L, null))
                .isInstanceOf(InvalidSupplierInvoiceStateException.class);
    }

    @Test
    void no_se_puede_abonar_una_anulada() {
        SupplierInvoice inv = invoice("1000", "0", "0");
        inv.cancel(7L, null);
        assertThatThrownBy(() -> inv.registerPayment(payment("10"), 7L, null))
                .isInstanceOf(InvalidSupplierInvoiceStateException.class);
    }

    @Test
    void anular_con_abonos_es_rechazado() {
        SupplierInvoice inv = invoice("1000", "0", "0");
        inv.registerPayment(payment("100"), 7L, null);
        assertThatThrownBy(() -> inv.cancel(7L, null))
                .isInstanceOf(InvalidSupplierInvoiceStateException.class);
    }

    @Test
    void anular_pending_deja_saldo_cero() {
        SupplierInvoice inv = invoice("1000", "0", "0");
        inv.cancel(7L, null);
        assertThat(inv.getStatus()).isEqualTo(SupplierInvoiceStatus.CANCELLED);
        assertThat(inv.balance()).isEqualByComparingTo("0");
    }

    @Test
    void vencimiento_anterior_a_emision_es_invalido() {
        assertThatThrownBy(() -> SupplierInvoice.create(CO, BR, SUP, null, null, "FV-2",
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), bd("100"), bd("0"), bd("0"),
                null, 7L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retencion_mayor_al_total_es_invalida() {
        assertThatThrownBy(() -> invoice("100", "19", "200"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Construye una factura variando un solo campo, para la matriz de invariantes.
     */
    private static SupplierInvoice construir(CompanyRef company, BranchRef branch,
            SupplierRef supplier, String invoiceNumber, LocalDate issueDate, LocalDate dueDate,
            BigDecimal subtotal, BigDecimal taxAmount, BigDecimal withholdingAmount,
            SupplierInvoiceStatus status, String notes) {
        return new SupplierInvoice(null, company, branch, supplier, null, null, invoiceNumber,
                issueDate, dueDate, subtotal, taxAmount, withholdingAmount, status, notes,
                List.of(), LocalDateTime.of(2026, 1, 15, 10, 30), 7L, null, null, null, true);
    }

    static Stream<Arguments> invariantesInvalidas() {
        LocalDate emision = LocalDate.of(2026, 7, 1);
        LocalDate vencimiento = LocalDate.of(2026, 7, 31);
        return Stream.of(
                Arguments.of("company nula",
                        (ThrowingCallable) () -> construir(null, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "company is required"),
                Arguments.of("branch nula",
                        (ThrowingCallable) () -> construir(CO, null, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "branch is required"),
                Arguments.of("supplier nulo",
                        (ThrowingCallable) () -> construir(CO, BR, null, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "supplier is required"),
                Arguments.of("invoiceNumber nulo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, null, emision, vencimiento,
                                bd("100"), bd("0"), bd("0"), SupplierInvoiceStatus.PENDING, null),
                        "invoiceNumber is required"),
                Arguments.of("invoiceNumber en blanco",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "   ", emision, vencimiento,
                                bd("100"), bd("0"), bd("0"), SupplierInvoiceStatus.PENDING, null),
                        "invoiceNumber is required"),
                Arguments.of("invoiceNumber excede 60 caracteres",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "F".repeat(61), emision,
                                vencimiento, bd("100"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "invoiceNumber must be 60 chars or less"),
                Arguments.of("issueDate nula",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", null, vencimiento,
                                bd("100"), bd("0"), bd("0"), SupplierInvoiceStatus.PENDING, null),
                        "issueDate is required"),
                Arguments.of("dueDate nula",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision, null,
                                bd("100"), bd("0"), bd("0"), SupplierInvoiceStatus.PENDING, null),
                        "dueDate is required"),
                Arguments.of("subtotal nulo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, null, bd("0"), bd("0"), SupplierInvoiceStatus.PENDING,
                                null),
                        "subtotal cannot be negative"),
                Arguments.of("subtotal negativo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("-1"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "subtotal cannot be negative"),
                Arguments.of("taxAmount nulo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), null, bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "taxAmount cannot be negative"),
                Arguments.of("taxAmount negativo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("-1"), bd("0"),
                                SupplierInvoiceStatus.PENDING, null),
                        "taxAmount cannot be negative"),
                Arguments.of("withholdingAmount nulo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), null,
                                SupplierInvoiceStatus.PENDING, null),
                        "withholdingAmount cannot be negative"),
                Arguments.of("withholdingAmount negativo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("-1"),
                                SupplierInvoiceStatus.PENDING, null),
                        "withholdingAmount cannot be negative"),
                Arguments.of("status nulo",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("0"), null, null),
                        "status is required"),
                Arguments.of("notes excede 500 caracteres",
                        (ThrowingCallable) () -> construir(CO, BR, SUP, "FV-1", emision,
                                vencimiento, bd("100"), bd("0"), bd("0"),
                                SupplierInvoiceStatus.PENDING, "N".repeat(501)),
                        "notes must be 500 chars or less"));
    }

    @Nested
    @DisplayName("validaciones del constructor")
    class Validaciones {

        @ParameterizedTest(name = "{0}")
        @DisplayName("cada invariante de cabecera se rechaza con su mensaje")
        @MethodSource("com.vetsoftware.app.supplierinvoice.domain.SupplierInvoiceTest#invariantesInvalidas")
        void invariante_de_constructor_es_rechazada(String descripcion, ThrowingCallable invocacion,
                String mensajeEsperado) {
            assertThatThrownBy(invocacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensajeEsperado);
        }
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("aplica los campos nuevos y recalcula el total bruto")
        void aplica_los_campos_nuevos_y_recalcula_el_total() {
            SupplierInvoice inv = invoice("1000", "190", "10");
            BranchRef otraSede = new BranchRef(20L, "Sede Norte");
            SupplierRef otroProveedor = new SupplierRef(6L, "Otro Proveedor", "900999888-1");

            inv.update(otraSede, otroProveedor, 100L, 200L, "FV-9", LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 31), bd("2000"), bd("380"), bd("20"), "nota nueva", 8L,
                    3L);

            assertThat(inv.getBranch()).isEqualTo(otraSede);
            assertThat(inv.getSupplier()).isEqualTo(otroProveedor);
            assertThat(inv.getPurchaseOrderId()).isEqualTo(100L);
            assertThat(inv.getGoodsReceiptId()).isEqualTo(200L);
            assertThat(inv.getInvoiceNumber()).isEqualTo("FV-9");
            assertThat(inv.getIssueDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(inv.getDueDate()).isEqualTo(LocalDate.of(2026, 8, 31));
            assertThat(inv.getTotal()).isEqualByComparingTo("2380");
            assertThat(inv.getUpdatedBy()).isEqualTo(8L);
            assertThat(inv.getVersion()).isEqualTo(3L);
        }

        /**
         * BE-fix3: {@code update(...)} validaba {@code notes} (lo pasaba a
         * {@code validate(...)}) pero nunca hacia {@code this.notes = notes} — el campo
         * quedaba congelado en el valor de creacion pasara lo que pasara en la edicion.
         * Ahora la asignacion existe y las notas nuevas se persisten.
         */
        @Test
        @DisplayName("las notas nuevas se validan y se asignan al campo")
        void las_notas_nuevas_se_asignan() {
            SupplierInvoice inv = SupplierInvoice.create(CO, BR, SUP, null, null, "FV-1",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), bd("1000"), bd("190"),
                    bd("10"), "nota original", 7L);

            inv.update(BR, SUP, null, null, "FV-1", LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 31), bd("1000"), bd("190"), bd("10"), "nota nueva", 7L,
                    1L);

            assertThat(inv.getNotes()).isEqualTo("nota nueva");
        }

        /**
         * Factura en el estado indicado, con abonos "de mentira" para simular el
         * escenario.
         */
        private SupplierInvoice invoiceEnEstado(SupplierInvoiceStatus estado) {
            return new SupplierInvoice(1L, CO, BR, SUP, null, null, "FV-1",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), bd("1000"), bd("190"),
                    bd("10"), estado, null, List.of(), LocalDateTime.of(2026, 1, 1, 0, 0), 7L, null,
                    null, 1L, true);
        }

        @ParameterizedTest(name = "no se puede editar en estado {0}")
        @DisplayName("solo una factura PENDING (sin abonos) admite edicion")
        @EnumSource(value = SupplierInvoiceStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
        void solo_una_factura_pending_admite_edicion(SupplierInvoiceStatus estadoNoPending) {
            SupplierInvoice inv = invoiceEnEstado(estadoNoPending);

            assertThatThrownBy(() -> inv.update(BR, SUP, null, null, "FV-2",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), bd("1000"), bd("190"),
                    bd("10"), null, 7L, 2L))
                    .isInstanceOf(InvalidSupplierInvoiceStateException.class)
                    .hasMessageContaining("can only be edited while PENDING");
        }
    }

    @Nested
    @DisplayName("habilitacion y atributos secundarios")
    class HabilitacionYAtributosSecundarios {

        @Test
        @DisplayName("nace habilitada y admite deshabilitarse/rehabilitarse")
        void nace_habilitada_y_admite_deshabilitarse_rehabilitarse() {
            SupplierInvoice inv = invoice("100", "0", "0");
            assertThat(inv.isEnabled()).isTrue();

            inv.disable();
            assertThat(inv.isEnabled()).isFalse();

            inv.enable();
            assertThat(inv.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("conserva las referencias opcionales de orden de compra y recepcion")
        void conserva_las_referencias_opcionales() {
            SupplierInvoice inv = SupplierInvoice.create(CO, BR, SUP, 55L, 66L, "FV-3",
                    LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), bd("100"), bd("0"),
                    bd("0"), null, 7L);

            assertThat(inv.getPurchaseOrderId()).isEqualTo(55L);
            assertThat(inv.getGoodsReceiptId()).isEqualTo(66L);
        }
    }
}
