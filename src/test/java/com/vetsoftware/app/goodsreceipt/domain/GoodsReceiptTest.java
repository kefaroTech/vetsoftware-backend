package com.vetsoftware.app.goodsreceipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.goodsreceipt.testsupport.GoodsReceiptMother;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

@DisplayName("GoodsReceipt — invariantes y transiciones de estado del agregado")
class GoodsReceiptTest {

    /**
     * Constructor de fixtures con un campo variable por caso. Evita repetir 16
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = GoodsReceiptMother.RECEIPT_ID;
        private CompanyRef company = GoodsReceiptMother.CLINICA;
        private BranchRef branch = GoodsReceiptMother.SEDE;
        private SupplierRef supplier = GoodsReceiptMother.PROVEEDOR;
        private Long purchaseOrderId;
        private LocalDate receiptDate = GoodsReceiptMother.FECHA_RECEPCION;
        private String supplierInvoiceNumber = "FV-1001";
        private String notes = "Entrega parcial";
        private GoodsReceiptStatus status = GoodsReceiptStatus.DRAFT;
        private List<GoodsReceiptLine> lines = List.of(GoodsReceiptMother.linea());

        private Builder company(CompanyRef v) {
            this.company = v;
            return this;
        }

        private Builder branch(BranchRef v) {
            this.branch = v;
            return this;
        }

        private Builder supplier(SupplierRef v) {
            this.supplier = v;
            return this;
        }

        private Builder receiptDate(LocalDate v) {
            this.receiptDate = v;
            return this;
        }

        private Builder supplierInvoiceNumber(String v) {
            this.supplierInvoiceNumber = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder status(GoodsReceiptStatus v) {
            this.status = v;
            return this;
        }

        private Builder lines(List<GoodsReceiptLine> v) {
            this.lines = v;
            return this;
        }

        private GoodsReceipt build() {
            return new GoodsReceipt(id, company, branch, supplier, purchaseOrderId, receiptDate,
                    supplierInvoiceNumber, notes, status, lines, GoodsReceiptMother.CREADO,
                    GoodsReceiptMother.ACTOR_ID, null, null, 0L, true);
        }
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        @Test
        @DisplayName("conserva cada campo tal cual se le entrega")
        void conserva_cada_campo() {
            GoodsReceipt receipt = valido().build();

            assertThat(receipt.getId()).isEqualTo(GoodsReceiptMother.RECEIPT_ID);
            assertThat(receipt.getCompany()).isEqualTo(GoodsReceiptMother.CLINICA);
            assertThat(receipt.getBranch()).isEqualTo(GoodsReceiptMother.SEDE);
            assertThat(receipt.getSupplier()).isEqualTo(GoodsReceiptMother.PROVEEDOR);
            assertThat(receipt.getPurchaseOrderId()).isNull();
            assertThat(receipt.getReceiptDate()).isEqualTo(GoodsReceiptMother.FECHA_RECEPCION);
            assertThat(receipt.getSupplierInvoiceNumber()).isEqualTo("FV-1001");
            assertThat(receipt.getNotes()).isEqualTo("Entrega parcial");
            assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(receipt.getLines()).hasSize(1);
            assertThat(receipt.getCreatedDate()).isEqualTo(GoodsReceiptMother.CREADO);
            assertThat(receipt.getCreatedBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(receipt.getUpdatedDate()).isNull();
            assertThat(receipt.getUpdatedBy()).isNull();
            assertThat(receipt.getVersion()).isZero();
            assertThat(receipt.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("copia las lineas: mutar la lista original no altera el agregado")
        void copia_las_lineas() {
            List<GoodsReceiptLine> originales = new ArrayList<>();
            originales.add(GoodsReceiptMother.linea());
            GoodsReceipt receipt = valido().lines(originales).build();

            originales.add(GoodsReceiptMother.lineaDeOrden(900L));

            assertThat(receipt.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("expone las lineas como lista inmutable")
        void expone_lineas_inmutables() {
            List<GoodsReceiptLine> lines = valido().build().getLines();

            assertThatThrownBy(() -> lines.add(GoodsReceiptMother.linea()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("acepta id nulo — el agregado aun no persistido es valido")
        void acepta_id_nulo() {
            GoodsReceipt receipt = new GoodsReceipt(null, GoodsReceiptMother.CLINICA,
                    GoodsReceiptMother.SEDE, GoodsReceiptMother.PROVEEDOR, null,
                    GoodsReceiptMother.FECHA_RECEPCION, null, null, GoodsReceiptStatus.DRAFT,
                    List.of(GoodsReceiptMother.linea()), GoodsReceiptMother.CREADO, null, null,
                    null, null, true);

            assertThat(receipt.getId()).isNull();
            assertThat(receipt.getSupplierInvoiceNumber()).isNull();
            assertThat(receipt.getNotes()).isNull();
        }

        static Stream<Arguments> construccionesInvalidas() {
            return Stream.of(arguments("empresa nula",
                    (ThrowingCallable) () -> valido().company(null).build(), "company is required"),
                    arguments("sede nula", (ThrowingCallable) () -> valido().branch(null).build(),
                            "branch is required"),
                    arguments("proveedor nulo",
                            (ThrowingCallable) () -> valido().supplier(null).build(),
                            "supplier is required"),
                    arguments("fecha de recepcion nula",
                            (ThrowingCallable) () -> valido().receiptDate(null).build(),
                            "receiptDate is required"),
                    arguments("estado nulo", (ThrowingCallable) () -> valido().status(null).build(),
                            "status is required"),
                    arguments("factura de 61 caracteres",
                            (ThrowingCallable) () -> valido().supplierInvoiceNumber("F".repeat(61))
                                    .build(),
                            "supplierInvoiceNumber must be 60 chars or less"),
                    arguments("notas de 501 caracteres",
                            (ThrowingCallable) () -> valido().notes("n".repeat(501)).build(),
                            "notes must be 500 chars or less"),
                    arguments("lineas nulas", (ThrowingCallable) () -> valido().lines(null).build(),
                            "at least one line is required"),
                    arguments("sin lineas",
                            (ThrowingCallable) () -> valido().lines(List.of()).build(),
                            "at least one line is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("construccionesInvalidas")
        @DisplayName("rechaza el agregado cuando falta un dato obligatorio")
        void rechaza_construcciones_invalidas(String caso, ThrowingCallable construccion,
                String mensaje) {
            assertThatThrownBy(construccion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta un numero de factura de exactamente 60 caracteres")
        void acepta_factura_de_60() {
            GoodsReceipt receipt = valido().supplierInvoiceNumber("F".repeat(60)).build();

            assertThat(receipt.getSupplierInvoiceNumber()).hasSize(60);
        }

        @Test
        @DisplayName("acepta notas de exactamente 500 caracteres")
        void acepta_notas_de_500() {
            GoodsReceipt receipt = valido().notes("n".repeat(500)).build();

            assertThat(receipt.getNotes()).hasSize(500);
        }
    }

    @Nested
    @DisplayName("Factory create")
    class Creacion {

        @Test
        @DisplayName("nace en DRAFT, habilitada, sin id ni datos de actualizacion")
        void nace_en_borrador() {
            GoodsReceipt receipt = GoodsReceipt.create(GoodsReceiptMother.CLINICA,
                    GoodsReceiptMother.SEDE, GoodsReceiptMother.PROVEEDOR,
                    GoodsReceiptMother.PURCHASE_ORDER_ID, GoodsReceiptMother.FECHA_RECEPCION,
                    "FV-1001", "Entrega parcial", List.of(GoodsReceiptMother.linea()),
                    GoodsReceiptMother.ACTOR_ID);

            assertThat(receipt.getId()).isNull();
            assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
            assertThat(receipt.isEnabled()).isTrue();
            assertThat(receipt.getVersion()).isNull();
            assertThat(receipt.getUpdatedDate()).isNull();
            assertThat(receipt.getUpdatedBy()).isNull();
            assertThat(receipt.getCreatedBy()).isEqualTo(GoodsReceiptMother.ACTOR_ID);
            assertThat(receipt.getPurchaseOrderId())
                    .isEqualTo(GoodsReceiptMother.PURCHASE_ORDER_ID);
            assertThat(receipt.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("aplica las mismas invariantes que el constructor")
        void aplica_las_mismas_invariantes() {
            assertThatThrownBy(
                    () -> GoodsReceipt.create(GoodsReceiptMother.CLINICA, GoodsReceiptMother.SEDE,
                            GoodsReceiptMother.PROVEEDOR, null, GoodsReceiptMother.FECHA_RECEPCION,
                            null, null, List.of(), GoodsReceiptMother.ACTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line is required");
        }

        @Test
        @DisplayName("createdDate queda sellado en el instante de creacion")
        void created_date_queda_sellado() {
            LocalDateTime antes = LocalDateTime.now().minusMinutes(1);

            GoodsReceipt receipt = GoodsReceipt.create(GoodsReceiptMother.CLINICA,
                    GoodsReceiptMother.SEDE, GoodsReceiptMother.PROVEEDOR, null,
                    GoodsReceiptMother.FECHA_RECEPCION, null, null,
                    List.of(GoodsReceiptMother.linea()), GoodsReceiptMother.ACTOR_ID);

            assertThat(receipt.getCreatedDate()).isAfter(antes);
        }
    }

    @Nested
    @DisplayName("Confirmacion")
    class Confirmacion {

        @Test
        @DisplayName("pasa de DRAFT a CONFIRMED y sella quien y cuando")
        void confirma_desde_borrador() {
            GoodsReceipt receipt = valido().build();

            receipt.confirm(55L);

            assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.CONFIRMED);
            assertThat(receipt.getUpdatedBy()).isEqualTo(55L);
            assertThat(receipt.getUpdatedDate()).isNotNull();
        }

        @ParameterizedTest(name = "desde {0}")
        @EnumSource(value = GoodsReceiptStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("rechaza confirmar dos veces: la guarda de idempotencia del inventario")
        void rechaza_confirmar_fuera_de_borrador(GoodsReceiptStatus status) {
            GoodsReceipt receipt = valido().status(status).build();

            assertThatThrownBy(() -> receipt.confirm(55L))
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                    .hasMessageContaining("Cannot transition goods receipt from " + status)
                    .hasMessageContaining("to CONFIRMED");

            assertThat(receipt.getStatus()).isEqualTo(status);
        }
    }

    @Nested
    @DisplayName("Cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("pasa de CONFIRMED a CANCELLED y sella quien y cuando")
        void cancela_desde_confirmada() {
            GoodsReceipt receipt = valido().status(GoodsReceiptStatus.CONFIRMED).build();

            receipt.cancel(55L);

            assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.CANCELLED);
            assertThat(receipt.getUpdatedBy()).isEqualTo(55L);
            assertThat(receipt.getUpdatedDate()).isNotNull();
        }

        @ParameterizedTest(name = "desde {0}")
        @EnumSource(value = GoodsReceiptStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("rechaza cancelar lo que no esta CONFIRMED")
        void rechaza_cancelar_fuera_de_confirmada(GoodsReceiptStatus status) {
            GoodsReceipt receipt = valido().status(status).build();

            assertThatThrownBy(() -> receipt.cancel(55L))
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class)
                    .hasMessageContaining("Cannot transition goods receipt from " + status)
                    .hasMessageContaining("to CANCELLED");

            assertThat(receipt.getStatus()).isEqualTo(status);
        }

        @Test
        @DisplayName("el ciclo completo DRAFT → CONFIRMED → CANCELLED es irreversible")
        void el_ciclo_completo_es_irreversible() {
            GoodsReceipt receipt = valido().build();

            receipt.confirm(55L);
            receipt.cancel(55L);

            assertThatThrownBy(() -> receipt.confirm(55L))
                    .isInstanceOf(InvalidGoodsReceiptStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("disable apaga la recepcion y enable la vuelve a encender")
        void disable_y_enable() {
            GoodsReceipt receipt = valido().build();

            receipt.disable();
            assertThat(receipt.isEnabled()).isFalse();

            receipt.enable();
            assertThat(receipt.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("apagar no cambia el estado del flujo")
        void apagar_no_cambia_el_estado() {
            GoodsReceipt receipt = valido().build();

            receipt.disable();

            assertThat(receipt.getStatus()).isEqualTo(GoodsReceiptStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Excepciones de la feature")
    class Excepciones {

        @Test
        @DisplayName("GoodsReceiptNotFoundException lleva el id que no se encontro")
        void not_found_lleva_el_id() {
            assertThat(new GoodsReceiptNotFoundException(404L))
                    .hasMessageContaining("Goods receipt not found: 404");
        }

        @Test
        @DisplayName("la transicion invalida acepta tambien un mensaje libre")
        void transicion_con_mensaje_libre() {
            assertThat(new InvalidGoodsReceiptStatusTransitionException("motivo libre"))
                    .hasMessage("motivo libre");
        }

        @Test
        @DisplayName("la transicion invalida compone el mensaje a partir de los dos estados")
        void transicion_compone_el_mensaje() {
            assertThat(new InvalidGoodsReceiptStatusTransitionException(GoodsReceiptStatus.DRAFT,
                    GoodsReceiptStatus.CANCELLED))
                    .hasMessage("Cannot transition goods receipt from DRAFT to CANCELLED");
        }
    }
}
