package com.vetsoftware.app.catalogitem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CatalogItem — invariantes del artículo del catálogo")
class CatalogItemTest {

    /** Artículo válido salvo por lo que cada test cambie. */
    private static CatalogItem item(String code, String name, ItemType itemType,
            CapacityUnit capacityUnit, int minQuantity, Integer maxQuantity, int sortOrder,
            CatalogItemStatus status) {
        return new CatalogItem(1L, code, name, null, null, itemType, capacityUnit, false,
                minQuantity, maxQuantity, sortOrder, status, CatalogItemMother.CREADO, 0L, true);
    }

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("create nace habilitado, sin id y con la fecha del reloj inyectado")
        void create_nace_habilitado_y_con_la_fecha_del_reloj() {
            CatalogItem item = CatalogItem.create("CORE", "Núcleo", null, null, ItemType.MODULE,
                    null, true, 1, 1, 0, CatalogItemStatus.ACTIVE, CatalogItemMother.RELOJ);

            assertThat(item.getId()).isNull();
            assertThat(item.getVersion()).isNull();
            assertThat(item.isEnabled()).isTrue();
            assertThat(item.getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        }

        @Test
        @DisplayName("create sin estado explícito nace en DRAFT, que es el default de la ficha")
        void create_sin_estado_nace_en_draft() {
            CatalogItem item = CatalogItem.create("CORE", "Núcleo", null, null, ItemType.MODULE,
                    null, false, 1, null, 0, null, CatalogItemMother.RELOJ);

            assertThat(item.getStatus()).isEqualTo(CatalogItemStatus.DRAFT);
        }

        @Test
        @DisplayName("isBundle solo es cierto para BUNDLE")
        void is_bundle_solo_para_bundle() {
            assertThat(CatalogItemMother.paqueteBasico().isBundle()).isTrue();
            assertThat(CatalogItemMother.historiaClinica().isBundle()).isFalse();
            assertThat(CatalogItemMother.usuarioExtra().isBundle()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("rechaza el código vacío o en blanco")
        void rechaza_codigo_en_blanco(String code) {
            assertThatThrownBy(() -> item(code, "Nombre", ItemType.MODULE, null, 1, null, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza el código nulo")
        void rechaza_codigo_nulo() {
            assertThatThrownBy(() -> item(null, "Nombre", ItemType.MODULE, null, 1, null, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("code is required");
        }

        @Test
        @DisplayName("rechaza el código de más de 50 caracteres, que es el ancho de la columna")
        void rechaza_codigo_demasiado_largo() {
            assertThatThrownBy(() -> item("C".repeat(51), "Nombre", ItemType.MODULE, null, 1, null,
                    0, CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("50 chars or less");
        }

        @Test
        @DisplayName("rechaza el nombre de más de 120 caracteres")
        void rechaza_nombre_demasiado_largo() {
            assertThatThrownBy(() -> item("CODE", "N".repeat(121), ItemType.MODULE, null, 1, null,
                    0, CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("120 chars or less");
        }

        @Test
        @DisplayName("rechaza la descripción corta de más de 255 caracteres")
        void rechaza_descripcion_corta_demasiado_larga() {
            assertThatThrownBy(() -> new CatalogItem(1L, "CODE", "Nombre", "D".repeat(256), null,
                    ItemType.MODULE, null, false, 1, null, 0, CatalogItemStatus.DRAFT,
                    CatalogItemMother.CREADO, 0L, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("255 chars or less");
        }

        @Test
        @DisplayName("rechaza un CAPACITY sin unidad: sería un contador sin qué contar")
        void rechaza_capacity_sin_unidad() {
            assertThatThrownBy(() -> item("CODE", "Nombre", ItemType.CAPACITY, null, 1, null, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("capacityUnit is required for CAPACITY");
        }

        @ParameterizedTest
        @EnumSource(value = ItemType.class, names = "CAPACITY", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("rechaza la unidad de capacidad en cualquier tipo que no sea CAPACITY")
        void rechaza_unidad_fuera_de_capacity(ItemType itemType) {
            assertThatThrownBy(() -> item("CODE", "Nombre", itemType, CapacityUnit.USER, 1, null, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only allowed on CAPACITY items");
        }

        @ParameterizedTest
        @EnumSource(CapacityUnit.class)
        @DisplayName("acepta cualquier unidad del dominio cerrado en un CAPACITY")
        void acepta_todas_las_unidades_en_capacity(CapacityUnit unit) {
            CatalogItem item = item("CODE", "Nombre", ItemType.CAPACITY, unit, 1, null, 0,
                    CatalogItemStatus.ACTIVE);

            assertThat(item.getCapacityUnit()).isEqualTo(unit);
        }

        @Test
        @DisplayName("rechaza la cantidad mínima negativa")
        void rechaza_cantidad_minima_negativa() {
            assertThatThrownBy(() -> item("CODE", "Nombre", ItemType.MODULE, null, -1, null, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("minQuantity cannot be negative");
        }

        @Test
        @DisplayName("rechaza un tope superior por debajo del inferior")
        void rechaza_maximo_menor_que_minimo() {
            assertThatThrownBy(() -> item("CODE", "Nombre", ItemType.MODULE, null, 5, 4, 0,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxQuantity cannot be lower");
        }

        @Test
        @DisplayName("acepta un tope superior igual al inferior y un tope ausente")
        void acepta_maximo_igual_o_ausente() {
            assertThat(
                    item("CODE", "Nombre", ItemType.MODULE, null, 5, 5, 0, CatalogItemStatus.DRAFT)
                            .getMaxQuantity())
                    .isEqualTo(5);
            assertThat(item("CODE", "Nombre", ItemType.MODULE, null, 5, null, 0,
                    CatalogItemStatus.DRAFT).getMaxQuantity()).isNull();
        }

        @Test
        @DisplayName("rechaza el orden de presentación negativo")
        void rechaza_orden_negativo() {
            assertThatThrownBy(() -> item("CODE", "Nombre", ItemType.MODULE, null, 1, null, -1,
                    CatalogItemStatus.DRAFT)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sortOrder cannot be negative");
        }

        @Test
        @DisplayName("rechaza el estado nulo")
        void rechaza_estado_nulo() {
            assertThatThrownBy(
                    () -> item("CODE", "Nombre", ItemType.MODULE, null, 1, null, 0, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("status is required");
        }

        @Test
        @DisplayName("rechaza el tipo nulo")
        void rechaza_tipo_nulo() {
            assertThatThrownBy(
                    () -> item("CODE", "Nombre", null, null, 1, null, 0, CatalogItemStatus.DRAFT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("itemType is required");
        }
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("update cambia lo comercial y deja intacto el código, que es inmutable")
        void update_no_toca_el_codigo() {
            CatalogItem item = CatalogItemMother.historiaClinica();

            item.update("Historia clínica PRO", "Otra corta", "Otra larga", ItemType.MODULE, null,
                    false, 2, 9, 3, CatalogItemStatus.DEPRECATED);

            assertThat(item.getCode()).isEqualTo("CLINICAL_HISTORY");
            assertThat(item.getName()).isEqualTo("Historia clínica PRO");
            assertThat(item.getShortDescription()).isEqualTo("Otra corta");
            assertThat(item.getLongDescription()).isEqualTo("Otra larga");
            assertThat(item.isCore()).isFalse();
            assertThat(item.getMinQuantity()).isEqualTo(2);
            assertThat(item.getMaxQuantity()).isEqualTo(9);
            assertThat(item.getSortOrder()).isEqualTo(3);
            assertThat(item.getStatus()).isEqualTo(CatalogItemStatus.DEPRECATED);
        }

        @Test
        @DisplayName("update aplica las mismas invariantes que el constructor")
        void update_aplica_las_mismas_invariantes() {
            CatalogItem item = CatalogItemMother.historiaClinica();

            assertThatThrownBy(() -> item.update("Nombre", null, null, ItemType.MODULE,
                    CapacityUnit.USER, false, 1, null, 0, CatalogItemStatus.ACTIVE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("only allowed on CAPACITY items");
        }

        @Test
        @DisplayName("update rechazado deja la entidad como estaba")
        void update_rechazado_no_muta_nada() {
            CatalogItem item = CatalogItemMother.historiaClinica();

            assertThatThrownBy(() -> item.update("", null, null, ItemType.MODULE, null, false, 1,
                    null, 0, CatalogItemStatus.ACTIVE))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(item.getName()).isEqualTo("Historia clínica");
        }

        @Test
        @DisplayName("enable y disable mueven el borrado lógico")
        void enable_y_disable() {
            CatalogItem item = CatalogItemMother.historiaClinica();

            item.disable();
            assertThat(item.isEnabled()).isFalse();

            item.enable();
            assertThat(item.isEnabled()).isTrue();
        }
    }
}
