package com.vetsoftware.app.inventory.domain;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COMPANY_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COUNT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.EMPLEADO_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.OTRO_PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.conteo;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.conteoQueCuadra;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.linea;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.vetsoftware.app.inventory.testsupport.InventoryMother;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("InventoryCount — la sesion de conteo fisico y sus diferencias")
class InventoryCountTest {

    private static InventoryCount conLineas(List<InventoryCountLine> lineas) {
        return new InventoryCount(COUNT_ID, COMPANY_ID, BRANCH_ID, null, EMPLEADO_ID,
                InventoryMother.CREADO, true, lineas);
    }

    @Nested
    @DisplayName("invariantes rechazadas")
    class Invariantes {

        @Test
        @DisplayName("exige la empresa")
        void exige_la_empresa() {
            assertThatThrownBy(() -> new InventoryCount(null, null, BRANCH_ID, null, EMPLEADO_ID,
                    null, true, List.of(linea(PRODUCT_ID, 10, 10))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("exige la sede: un conteo sin sede no se puede ajustar contra nada")
        void exige_la_sede() {
            assertThatThrownBy(() -> new InventoryCount(null, COMPANY_ID, null, null, EMPLEADO_ID,
                    null, true, List.of(linea(PRODUCT_ID, 10, 10))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("branchId is required");
        }

        @Test
        @DisplayName("un conteo sin lineas no es un conteo")
        void un_conteo_sin_lineas_no_es_un_conteo() {
            assertThatThrownBy(() -> conLineas(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("un conteo requiere al menos una línea");
        }

        @Test
        @DisplayName("una lista de lineas null tampoco pasa")
        void una_lista_de_lineas_null_tampoco_pasa() {
            assertThatThrownBy(() -> conLineas(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("un conteo requiere al menos una línea");
        }

        @Test
        @DisplayName("un producto repetido se rechaza con su id en el mensaje")
        void un_producto_repetido_se_rechaza() {
            // Dos lineas del mismo producto generarian dos ajustes sobre el mismo saldo:
            // el segundo partiria de un sistema que el primero ya movio, y el kardex
            // quedaria con un ajuste inventado.
            assertThatThrownBy(
                    () -> conLineas(List.of(linea(PRODUCT_ID, 10, 13), linea(PRODUCT_ID, 10, 7))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("producto repetido en el conteo: " + PRODUCT_ID);
        }

        @Test
        @DisplayName("dos productos distintos con la misma cantidad si pasan")
        void dos_productos_distintos_con_la_misma_cantidad_si_pasan() {
            assertThatCode(() -> conLineas(
                    List.of(linea(PRODUCT_ID, 10, 13), linea(OTRO_PRODUCT_ID, 10, 13))))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("contadores del resumen")
    class Contadores {

        @Test
        @DisplayName("cuenta todas las lineas contadas")
        void cuenta_todas_las_lineas_contadas() {
            assertThat(conteo().totalLines()).isEqualTo(3);
        }

        @Test
        @DisplayName("solo cuenta como ajustadas las lineas con diferencia")
        void solo_cuenta_como_ajustadas_las_lineas_con_diferencia() {
            // De las tres lineas, una sobra (+3) y otra falta (−2); la tercera cuadra.
            assertThat(conteo().adjustedLines()).isEqualTo(2);
        }

        @Test
        @DisplayName("un conteo que cuadra entero no ajusta nada")
        void un_conteo_que_cuadra_entero_no_ajusta_nada() {
            assertThat(conteoQueCuadra().adjustedLines()).isZero();
            assertThat(conteoQueCuadra().totalLines()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("create y ciclo de vida")
    class Creacion {

        @Test
        @DisplayName("nace sin id, habilitado y con la fecha del momento")
        void nace_sin_id_habilitado_y_con_la_fecha_del_momento() {
            InventoryCount count = InventoryCount.create(COMPANY_ID, BRANCH_ID, "Ciclico",
                    EMPLEADO_ID, List.of(linea(PRODUCT_ID, 10, 13)));

            assertThat(count.getId()).isNull();
            assertThat(count.isEnabled()).isTrue();
            assertThat(count.getNote()).isEqualTo("Ciclico");
            assertThat(count.getCountedBy()).isEqualTo(EMPLEADO_ID);
            // createdDate lo pone LocalDateTime.now() dentro del factory: no hay Clock
            // inyectable, asi que la asercion tiene que ser una ventana. Deuda anotada en
            // "Determinismo" del CLAUDE.md.
            assertThat(count.getCreatedDate()).isCloseTo(LocalDateTime.now(),
                    within(10, ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("assignId fija el id que devolvio la base")
        void assign_id_fija_el_id_de_la_base() {
            InventoryCount count = InventoryCount.create(COMPANY_ID, BRANCH_ID, null, EMPLEADO_ID,
                    List.of(linea(PRODUCT_ID, 10, 13)));

            count.assignId(42L);

            assertThat(count.getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("disable apaga la sesion sin borrarla")
        void disable_apaga_la_sesion_sin_borrarla() {
            InventoryCount count = conteo();

            count.disable();

            // El conteo es rastro de auditoria: se apaga, no se borra, porque los ajustes
            // que genero siguen en el kardex apuntando a su id.
            assertThat(count.isEnabled()).isFalse();
            assertThat(count.getLines()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("las lineas son inmutables desde fuera")
    class Inmutabilidad {

        @Test
        @DisplayName("la lista que se expone no se puede modificar")
        void la_lista_expuesta_no_se_puede_modificar() {
            assertThatThrownBy(() -> conteo().getLines().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("modificar la lista original despues no cambia el conteo")
        void modificar_la_lista_original_no_cambia_el_conteo() {
            List<InventoryCountLine> original = new ArrayList<>(List.of(linea(PRODUCT_ID, 10, 13)));
            InventoryCount count = conLineas(original);

            original.add(linea(OTRO_PRODUCT_ID, 8, 6));

            // El constructor copia: si guardara la referencia, quien construyo el conteo
            // podria colarle una linea despues de que las invariantes ya se validaron.
            assertThat(count.getLines()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("InventoryCountLine")
    class Linea {

        @ParameterizedTest(name = "sistema {0}, contado {1} → {2}")
        @CsvSource({"10, 13, 3", "8, 6, -2", "5, 5, 0", "0, 4, 4", "3, 0, -3"})
        @DisplayName("la diferencia es contado menos sistema")
        void la_diferencia_es_contado_menos_sistema(int sistema, int contado, int diferencia) {
            assertThat(linea(PRODUCT_ID, sistema, contado).difference()).isEqualTo(diferencia);
        }

        @Test
        @DisplayName("un sistema negativo es valido: la empresa puede permitir stock negativo")
        void un_sistema_negativo_es_valido() {
            // El saldo de sistema puede venir en negativo si la empresa permite vender
            // sin stock; el conteo tiene que poder corregirlo, no rechazarlo.
            assertThat(linea(PRODUCT_ID, -2, 5).difference()).isEqualTo(7);
        }

        @Test
        @DisplayName("contar una cantidad negativa no tiene sentido fisico")
        void contar_una_cantidad_negativa_no_tiene_sentido_fisico() {
            assertThatThrownBy(() -> InventoryCountLine.create(PRODUCT_ID, 10, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("countedQuantity cannot be negative");
        }

        @Test
        @DisplayName("exige el producto")
        void exige_el_producto() {
            assertThatThrownBy(() -> InventoryCountLine.create(null, 10, 13))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("productId is required");
        }

        @Test
        @DisplayName("nace sin id y lo recibe al persistirse")
        void nace_sin_id_y_lo_recibe_al_persistirse() {
            InventoryCountLine linea = InventoryCountLine.create(PRODUCT_ID, 10, 13);

            assertThat(linea.getId()).isNull();
            assertThat(linea.getSystemQuantity()).isEqualTo(10);
            assertThat(linea.getCountedQuantity()).isEqualTo(13);

            linea.assignId(42L);

            assertThat(linea.getId()).isEqualTo(42L);
        }
    }
}
