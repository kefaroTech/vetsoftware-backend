package com.vetsoftware.app.inventory.application.dto;

import static com.vetsoftware.app.inventory.testsupport.InventoryMother.BRANCH_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.COUNT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.EMPLEADO_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.OTRO_PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.PRODUCT_ID;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.conteo;
import static com.vetsoftware.app.inventory.testsupport.InventoryMother.linea;
import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.inventory.testsupport.InventoryMother;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DTOs de inventory")
class InventoryCountViewTest {

    @Nested
    @DisplayName("InventoryCountView.from — el detalle del conteo")
    class Detalle {

        @Test
        @DisplayName("traslada la cabecera y los dos contadores")
        void traslada_la_cabecera_y_los_contadores() {
            InventoryCountView vista = InventoryCountView.from(conteo());

            assertThat(vista.id()).isEqualTo(COUNT_ID);
            assertThat(vista.branchId()).isEqualTo(BRANCH_ID);
            assertThat(vista.note()).isEqualTo("Conteo ciclico de enero");
            assertThat(vista.countedBy()).isEqualTo(EMPLEADO_ID);
            assertThat(vista.createdDate()).isEqualTo(InventoryMother.CREADO);
            assertThat(vista.totalLines()).isEqualTo(3);
            assertThat(vista.adjustedLines()).isEqualTo(2);
        }

        @Test
        @DisplayName("proyecta cada linea con su diferencia ya calculada")
        void proyecta_cada_linea_con_su_diferencia() {
            InventoryCountView vista = InventoryCountView.from(conteo());

            assertThat(vista.lines()).extracting(InventoryCountLineView::productId).containsExactly(
                    PRODUCT_ID, OTRO_PRODUCT_ID, InventoryMother.TERCER_PRODUCT_ID);
            assertThat(vista.lines()).extracting(InventoryCountLineView::difference)
                    .containsExactly(3, -2, 0);
        }

        @Test
        @DisplayName("la linea lleva sistema y contado, no solo la diferencia")
        void la_linea_lleva_sistema_y_contado() {
            InventoryCountLineView vista = InventoryCountLineView.from(linea(PRODUCT_ID, 10, 13));

            // Enseñar solo el ajuste obligaria a recalcular de donde salio; el arqueo
            // de inventario tiene que poder leerse de una fila.
            assertThat(vista.systemQuantity()).isEqualTo(10);
            assertThat(vista.countedQuantity()).isEqualTo(13);
            assertThat(vista.difference()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("InventoryCountView.summary — la fila del listado")
    class Resumen {

        @Test
        @DisplayName("no arrastra las lineas: el historial solo necesita los contadores")
        void no_arrastra_las_lineas() {
            InventoryCountView resumen = InventoryCountView.summary(COUNT_ID, BRANCH_ID, "Ciclico",
                    EMPLEADO_ID, InventoryMother.CREADO, 3, 2);

            // Un conteo de inventario completo tiene cientos de lineas: traerlas en el
            // listado multiplicaria por N las filas de una pantalla que solo resume.
            assertThat(resumen.lines()).isEmpty();
            assertThat(resumen.totalLines()).isEqualTo(3);
            assertThat(resumen.adjustedLines()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("PageResult")
    class Pagina {

        @Test
        @DisplayName("map transforma el contenido y conserva los metadatos de la pagina")
        void map_transforma_el_contenido_y_conserva_los_metadatos() {
            PageResult<Integer> pagina = new PageResult<>(List.of(1, 2, 3), 2, 20, 41L, 3);

            PageResult<String> mapeada = pagina.map(String::valueOf);

            assertThat(mapeada.content()).containsExactly("1", "2", "3");
            assertThat(mapeada.page()).isEqualTo(2);
            assertThat(mapeada.pageSize()).isEqualTo(20);
            assertThat(mapeada.totalElements()).isEqualTo(41L);
            assertThat(mapeada.totalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("una pagina vacia se mapea a una pagina vacia, no a null")
        void una_pagina_vacia_se_mapea_a_vacia() {
            PageResult<Integer> vacia = new PageResult<>(List.of(), 0, 20, 0L, 0);

            assertThat(vacia.map(Function.identity()).content()).isEmpty();
        }
    }
}
