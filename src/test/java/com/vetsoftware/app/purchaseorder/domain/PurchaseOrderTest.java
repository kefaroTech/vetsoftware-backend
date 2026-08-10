package com.vetsoftware.app.purchaseorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.vetsoftware.app.purchaseorder.testsupport.PurchaseOrderMother;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("PurchaseOrder — invariantes y ciclo de vida del agregado")
class PurchaseOrderTest {

    /**
     * Constructor de fixtures con un campo variable por caso: evita repetir quince
     * argumentos en cada escenario invalido, que es como se cuela un test que
     * valida un campo distinto del que dice validar.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static PurchaseOrderLine lineaDeVacuna() {
        return PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0);
    }

    private static final class Builder {
        private Long id = 1L;
        private CompanyRef company = PurchaseOrderMother.CLINICA;
        private BranchRef branch = PurchaseOrderMother.SEDE_NORTE;
        private SupplierRef supplier = PurchaseOrderMother.PROVEEDOR;
        private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;
        private LocalDate orderDate = PurchaseOrderMother.FECHA_ORDEN;
        private LocalDate expectedDate = PurchaseOrderMother.FECHA_ESPERADA;
        private String notes = "Pedido mensual";
        private List<PurchaseOrderLine> lines = List.of(lineaDeVacuna());

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

        private Builder status(PurchaseOrderStatus v) {
            this.status = v;
            return this;
        }

        private Builder orderDate(LocalDate v) {
            this.orderDate = v;
            return this;
        }

        private Builder notes(String v) {
            this.notes = v;
            return this;
        }

        private Builder lines(List<PurchaseOrderLine> v) {
            this.lines = v;
            return this;
        }

        private PurchaseOrder build() {
            return new PurchaseOrder(id, company, branch, supplier, status, orderDate, expectedDate,
                    notes, lines, PurchaseOrderMother.CREADO, PurchaseOrderMother.ACTOR_ID, null,
                    null, 0L, true);
        }
    }

    @Nested
    @DisplayName("Construccion")
    class Construccion {

        private static Stream<Arguments> agregadosInvalidos() {
            return Stream.of(
                    arguments("empresa nula", valido().company(null), "company is required"),
                    arguments("sede nula", valido().branch(null), "branch is required"),
                    arguments("proveedor nulo", valido().supplier(null), "supplier is required"),
                    arguments("estado nulo", valido().status(null), "status is required"),
                    arguments("fecha de orden nula", valido().orderDate(null),
                            "orderDate is required"),
                    arguments("notas de 501 caracteres", valido().notes("x".repeat(501)),
                            "notes must be 500 chars or less"),
                    arguments("lista de lineas nula", valido().lines(null), "at least one line"),
                    arguments("lista de lineas vacia", valido().lines(List.of()),
                            "at least one line"),
                    arguments("linea nula dentro de la lista",
                            valido().lines(Arrays.asList(lineaDeVacuna(), null)),
                            "purchase order line is required"),
                    arguments("mismo producto en dos lineas", valido().lines(List.of(
                            PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0),
                            PurchaseOrderMother.linea(101L, PurchaseOrderMother.VACUNA, 3, 0))),
                            "duplicate product in purchase order lines: 11"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("agregadosInvalidos")
        @DisplayName("rechaza construir la orden cuando un dato obligatorio no cumple")
        void rechaza_agregado_invalido(String caso, Builder builder, String mensaje) {
            assertThatThrownBy(builder::build).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("acepta notas de exactamente 500 caracteres (limite superior inclusivo)")
        void acepta_notas_de_500_caracteres() {
            PurchaseOrder order = valido().notes("x".repeat(500)).build();

            assertThat(order.getNotes()).hasSize(500);
        }

        @Test
        @DisplayName("acepta notas nulas porque el campo es opcional")
        void acepta_notas_nulas() {
            assertThatCode(() -> valido().notes(null).build()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("acepta dos lineas de productos distintos")
        void acepta_dos_productos_distintos() {
            PurchaseOrder order = valido()
                    .lines(List.of(
                            PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0),
                            PurchaseOrderMother.linea(200L, PurchaseOrderMother.JERINGA, 4, 0)))
                    .build();

            assertThat(order.getLines()).hasSize(2);
        }

        @Test
        @DisplayName("create deja la orden en DRAFT, habilitada, sin version ni datos de edicion")
        void create_deja_la_orden_en_draft() {
            PurchaseOrder order = PurchaseOrder.create(PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR,
                    PurchaseOrderMother.FECHA_ORDEN, PurchaseOrderMother.FECHA_ESPERADA, "nota",
                    List.of(PurchaseOrderMother.lineaNueva()), PurchaseOrderMother.ACTOR_ID);

            assertThat(order.getId()).isNull();
            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
            assertThat(order.isEnabled()).isTrue();
            assertThat(order.getVersion()).isNull();
            assertThat(order.getUpdatedDate()).isNull();
            assertThat(order.getUpdatedBy()).isNull();
            assertThat(order.getCreatedBy()).isEqualTo(PurchaseOrderMother.ACTOR_ID);
            assertThat(order.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("copia la lista de lineas: mutar la original no altera el agregado")
        void copia_la_lista_de_lineas_recibida() {
            List<PurchaseOrderLine> origen = new ArrayList<>(List.of(lineaDeVacuna()));
            PurchaseOrder order = valido().lines(origen).build();

            origen.clear();

            assertThat(order.getLines()).hasSize(1);
        }

        @Test
        @DisplayName("getLines devuelve una copia inmodificable")
        void get_lines_devuelve_copia_inmodificable() {
            PurchaseOrder order = valido().build();

            assertThatThrownBy(() -> order.getLines().add(lineaDeVacuna()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("conserva cabecera y auditoria tal cual se construyo")
        void conserva_cabecera_y_auditoria() {
            PurchaseOrder order = valido().build();

            assertThat(order.getId()).isEqualTo(1L);
            assertThat(order.getCompany()).isEqualTo(PurchaseOrderMother.CLINICA);
            assertThat(order.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_NORTE);
            assertThat(order.getSupplier()).isEqualTo(PurchaseOrderMother.PROVEEDOR);
            assertThat(order.getOrderDate()).isEqualTo(PurchaseOrderMother.FECHA_ORDEN);
            assertThat(order.getExpectedDate()).isEqualTo(PurchaseOrderMother.FECHA_ESPERADA);
            assertThat(order.getCreatedDate()).isEqualTo(PurchaseOrderMother.CREADO);
            assertThat(order.getVersion()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Edicion")
    class Edicion {

        @Test
        @DisplayName("update en DRAFT reemplaza cabecera, lineas, auditoria y version esperada")
        void update_en_draft_reemplaza_todo() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            order.update(PurchaseOrderMother.SEDE_SUR, PurchaseOrderMother.OTRO_PROVEEDOR,
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 15), "nueva nota",
                    List.of(PurchaseOrderMother.linea(null, PurchaseOrderMother.JERINGA, 7, 0)),
                    55L, 4L);

            assertThat(order.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_SUR);
            assertThat(order.getSupplier()).isEqualTo(PurchaseOrderMother.OTRO_PROVEEDOR);
            assertThat(order.getOrderDate()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(order.getExpectedDate()).isEqualTo(LocalDate.of(2026, 4, 15));
            assertThat(order.getNotes()).isEqualTo("nueva nota");
            assertThat(order.getLines()).singleElement().extracting(PurchaseOrderLine::getProduct)
                    .isEqualTo(PurchaseOrderMother.JERINGA);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
            assertThat(order.getUpdatedDate()).isNotNull();
            assertThat(order.getVersion()).isEqualTo(4L);
        }

        @ParameterizedTest(name = "estado {0}")
        @EnumSource(value = PurchaseOrderStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("update fuera de DRAFT es un cambio de estado invalido")
        void update_fuera_de_draft_falla(PurchaseOrderStatus status) {
            PurchaseOrder order = PurchaseOrderMother.enEstado(status,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)));

            assertThatThrownBy(() -> order.update(PurchaseOrderMother.SEDE_SUR,
                    PurchaseOrderMother.PROVEEDOR, PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(PurchaseOrderMother.lineaNueva()), 55L, 1L))
                    .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                    .hasMessageContaining("only be edited while in DRAFT");
        }

        @Test
        @DisplayName("update revalida las invariantes: rechaza quedarse sin lineas")
        void update_rechaza_lista_de_lineas_vacia() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            assertThatThrownBy(() -> order.update(PurchaseOrderMother.SEDE_NORTE,
                    PurchaseOrderMother.PROVEEDOR, PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(), 55L, 1L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one line");
        }

        @Test
        @DisplayName("update rechaza repetir producto entre lineas")
        void update_rechaza_producto_repetido() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            assertThatThrownBy(() -> order.update(PurchaseOrderMother.SEDE_NORTE,
                    PurchaseOrderMother.PROVEEDOR, PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(PurchaseOrderMother.lineaNueva(), PurchaseOrderMother.lineaNueva()),
                    55L, 1L)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duplicate product");
        }

        @Test
        @DisplayName("update que falla no deja el agregado a medias")
        void update_fallido_no_modifica_el_agregado() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            assertThatThrownBy(() -> order.update(PurchaseOrderMother.SEDE_SUR,
                    PurchaseOrderMother.OTRO_PROVEEDOR, null, null, null,
                    List.of(PurchaseOrderMother.lineaNueva()), 55L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(order.getBranch()).isEqualTo(PurchaseOrderMother.SEDE_NORTE);
            assertThat(order.getSupplier()).isEqualTo(PurchaseOrderMother.PROVEEDOR);
            assertThat(order.getUpdatedBy()).isNull();
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("place mueve DRAFT a PLACED y sella quien la emitio")
        void place_mueve_draft_a_placed() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            order.place(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
            assertThat(order.getUpdatedDate()).isNotNull();
        }

        @ParameterizedTest(name = "desde {0}")
        @EnumSource(value = PurchaseOrderStatus.class, names = "DRAFT", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("place solo es valido desde DRAFT")
        void place_fuera_de_draft_falla(PurchaseOrderStatus status) {
            PurchaseOrder order = PurchaseOrderMother.enEstado(status,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)));

            assertThatThrownBy(() -> order.place(55L))
                    .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                    .hasMessageContaining(status + " -> PLACED");
        }

        @ParameterizedTest(name = "desde {0}")
        @EnumSource(value = PurchaseOrderStatus.class, names = {"DRAFT", "PLACED"})
        @DisplayName("cancel es valido desde DRAFT y desde PLACED si nada se recibio")
        void cancel_desde_draft_y_placed(PurchaseOrderStatus status) {
            PurchaseOrder order = PurchaseOrderMother.enEstado(status,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)));

            order.cancel(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.CANCELLED);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
        }

        @ParameterizedTest(name = "desde {0}")
        @EnumSource(value = PurchaseOrderStatus.class, names = {"DRAFT",
                "PLACED"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("cancel no es valido una vez recibida o ya cancelada")
        void cancel_fuera_de_draft_y_placed_falla(PurchaseOrderStatus status) {
            PurchaseOrder order = PurchaseOrderMother.enEstado(status,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)));

            assertThatThrownBy(() -> order.cancel(55L))
                    .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                    .hasMessageContaining(status + " -> CANCELLED");
        }

        @Test
        @DisplayName("cancel se bloquea si alguna linea ya tiene mercancia recibida")
        void cancel_con_mercancia_recibida_falla() {
            PurchaseOrder order = PurchaseOrderMother.enEstado(PurchaseOrderStatus.PLACED,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 1)));

            assertThatThrownBy(() -> order.cancel(55L))
                    .isInstanceOf(InvalidPurchaseOrderStatusTransitionException.class)
                    .hasMessageContaining("already has received items");

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
        }

        @Test
        @DisplayName("enable y disable togglean el soft-delete sin tocar el estado")
        void enable_y_disable_togglean_el_soft_delete() {
            PurchaseOrder order = PurchaseOrderMother.borrador();

            order.disable();
            assertThat(order.isEnabled()).isFalse();

            order.enable();
            assertThat(order.isEnabled()).isTrue();
            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("Recepcion de mercancia")
    class Recepcion {

        @Test
        @DisplayName("receiveLine acumula lo recibido en la linea indicada")
        void receive_line_acumula_en_la_linea() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();

            order.receiveLine(100L, 4);

            assertThat(order.getLines().get(0).getQuantityReceived()).isEqualTo(4);
            assertThat(order.getLines().get(1).getQuantityReceived()).isZero();
        }

        @Test
        @DisplayName("receiveLine con id nulo se rechaza")
        void receive_line_con_id_nulo_falla() {
            PurchaseOrder order = PurchaseOrderMother.emitida();

            assertThatThrownBy(() -> order.receiveLine(null, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("purchase order line id is required");
        }

        @Test
        @DisplayName("receiveLine de una linea que no pertenece a la orden se rechaza")
        void receive_line_de_linea_ajena_falla() {
            PurchaseOrder order = PurchaseOrderMother.emitida();

            assertThatThrownBy(() -> order.receiveLine(999L, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Purchase order line 999 not found in order 1");
        }

        @Test
        @DisplayName("revertLine descuenta lo recibido de la linea indicada")
        void revert_line_descuenta_lo_recibido() {
            PurchaseOrder order = PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)));

            order.revertLine(100L, 4);

            assertThat(order.getLines().get(0).getQuantityReceived()).isEqualTo(6);
        }

        @Test
        @DisplayName("recibir todo lo pedido en todas las lineas deja la orden en RECEIVED")
        void recalcular_tras_recibir_todo_deja_received() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();
            order.receiveLine(100L, 10);
            order.receiveLine(200L, 4);

            order.recalculateStatusAfterReceipt(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
        }

        @Test
        @DisplayName("recibir solo una parte deja la orden en PARTIALLY_RECEIVED")
        void recalcular_tras_recibir_parte_deja_partially_received() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();
            order.receiveLine(100L, 3);

            order.recalculateStatusAfterReceipt(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        @Test
        @DisplayName("recalcular sin nada recibido conserva el estado y solo sella la auditoria")
        void recalcular_sin_recepciones_conserva_el_estado() {
            PurchaseOrder order = PurchaseOrderMother.emitidaConDosLineas();

            order.recalculateStatusAfterReceipt(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
            assertThat(order.getUpdatedBy()).isEqualTo(55L);
        }

        @Test
        @DisplayName("revertir parcialmente deja la orden en PARTIALLY_RECEIVED")
        void recalcular_tras_revertir_parte_deja_partially_received() {
            PurchaseOrder order = PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)));
            order.revertLine(100L, 4);

            order.recalculateStatusAfterRevert(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        @Test
        @DisplayName("revertir la totalidad devuelve la orden a PLACED")
        void recalcular_tras_revertir_todo_vuelve_a_placed() {
            PurchaseOrder order = PurchaseOrderMother.enEstado(PurchaseOrderStatus.RECEIVED,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 10)));
            order.revertLine(100L, 10);

            order.recalculateStatusAfterRevert(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
        }

        @Test
        @DisplayName("una orden con lineas de cantidad recibida cero se considera no recibida")
        void recalcular_tras_revertir_sin_recepciones_vuelve_a_placed() {
            PurchaseOrder order = PurchaseOrderMother.enEstado(
                    PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    List.of(PurchaseOrderMother.linea(100L, PurchaseOrderMother.VACUNA, 10, 0)));

            order.recalculateStatusAfterRevert(55L);

            assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PLACED);
        }
    }

    @Nested
    @DisplayName("Excepciones de dominio")
    class Excepciones {

        @Test
        @DisplayName("la excepcion de transicion nombra origen y destino")
        void la_excepcion_de_transicion_nombra_origen_y_destino() {
            InvalidPurchaseOrderStatusTransitionException ex = new InvalidPurchaseOrderStatusTransitionException(
                    PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.CANCELLED);

            assertThat(ex).hasMessageContaining("RECEIVED -> CANCELLED");
        }

        @Test
        @DisplayName("la excepcion de no encontrada lleva el id buscado")
        void la_excepcion_de_no_encontrada_lleva_el_id() {
            assertThat(new PurchaseOrderNotFoundException(42L))
                    .hasMessageContaining("Purchase order not found: 42");
        }
    }

    @Nested
    @DisplayName("Companion VOs")
    class CompanionVos {

        private static Stream<Arguments> refsInvalidas() {
            return Stream.of(
                    arguments("company sin id",
                            (ThrowingSupplier) () -> new CompanyRef(null, "n", "900"),
                            "company id is required"),
                    arguments("company sin nombre",
                            (ThrowingSupplier) () -> new CompanyRef(1L, " ", "900"),
                            "company name is required"),
                    arguments("company sin identificador",
                            (ThrowingSupplier) () -> new CompanyRef(1L, "n", ""),
                            "company identifier is required"),
                    arguments("branch sin id", (ThrowingSupplier) () -> new BranchRef(null, "n"),
                            "branch id is required"),
                    arguments("branch sin nombre", (ThrowingSupplier) () -> new BranchRef(1L, null),
                            "branch name is required"),
                    arguments("supplier sin id",
                            (ThrowingSupplier) () -> new SupplierRef(null, "n"),
                            "supplier id is required"),
                    arguments("supplier sin nombre",
                            (ThrowingSupplier) () -> new SupplierRef(1L, "  "),
                            "supplier name is required"),
                    arguments("product sin id",
                            (ThrowingSupplier) () -> new ProductRef(null, "n", "c"),
                            "product id is required"),
                    arguments("product sin nombre",
                            (ThrowingSupplier) () -> new ProductRef(1L, "", "c"),
                            "product name is required"),
                    arguments("product sin codigo",
                            (ThrowingSupplier) () -> new ProductRef(1L, "n", null),
                            "product code is required"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("refsInvalidas")
        @DisplayName("los companion VO rechazan referencias incompletas")
        void los_companion_vo_rechazan_referencias_incompletas(String caso,
                ThrowingSupplier constructor, String mensaje) {
            assertThatThrownBy(constructor::get).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(mensaje);
        }

        @Test
        @DisplayName("los companion VO validos exponen sus datos")
        void los_companion_vo_validos_exponen_sus_datos() {
            assertThat(PurchaseOrderMother.CLINICA.identifier()).isEqualTo("900123456");
            assertThat(PurchaseOrderMother.SEDE_NORTE.name()).isEqualTo("Sede Norte");
            assertThat(PurchaseOrderMother.PROVEEDOR.id()).isEqualTo(7L);
            assertThat(PurchaseOrderMother.VACUNA.code()).isEqualTo("VAC-001");
        }
    }

    /** Fabrica de un VO para los casos parametrizados de construccion invalida. */
    @FunctionalInterface
    interface ThrowingSupplier {
        Object get();
    }

    @Nested
    @DisplayName("Estado del ciclo de vida")
    class Estados {

        @ParameterizedTest(name = "{0}")
        @EnumSource(PurchaseOrderStatus.class)
        @DisplayName("cada estado del enum sobrevive al round-trip por nombre")
        void cada_estado_sobrevive_al_round_trip(PurchaseOrderStatus status) {
            assertThat(PurchaseOrderStatus.valueOf(status.name())).isSameAs(status);
        }

        @Test
        @DisplayName("el enum declara exactamente los cinco estados del ciclo de vida")
        void el_enum_declara_los_cinco_estados() {
            assertThat(PurchaseOrderStatus.values()).containsExactly(PurchaseOrderStatus.DRAFT,
                    PurchaseOrderStatus.PLACED, PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    PurchaseOrderStatus.RECEIVED, PurchaseOrderStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Costos de las lineas")
    class CostosDeLasLineas {

        @Test
        @DisplayName("la orden conserva el costo unitario de cada linea sin redondear")
        void conserva_el_costo_unitario_de_cada_linea() {
            PurchaseOrder order = valido().lines(List.of(new PurchaseOrderLine(100L,
                    PurchaseOrderMother.VACUNA, 3, new BigDecimal("1234.56"), 0))).build();

            assertThat(order.getLines().get(0).getUnitCost())
                    .isEqualByComparingTo(new BigDecimal("1234.56"));
        }

        @Test
        @DisplayName("la fecha de creacion de create es la del reloj del sistema, no nula")
        void create_sella_la_fecha_de_creacion() {
            LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

            PurchaseOrder order = PurchaseOrder.create(PurchaseOrderMother.CLINICA,
                    PurchaseOrderMother.SEDE_NORTE, PurchaseOrderMother.PROVEEDOR,
                    PurchaseOrderMother.FECHA_ORDEN, null, null,
                    List.of(PurchaseOrderMother.lineaNueva()), PurchaseOrderMother.ACTOR_ID);

            assertThat(order.getCreatedDate()).isAfter(antes);
        }
    }
}
