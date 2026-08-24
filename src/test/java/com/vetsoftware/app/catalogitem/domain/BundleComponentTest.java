package com.vetsoftware.app.catalogitem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("BundleComponent — invariantes de una pieza de paquete")
class BundleComponentTest {

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("create nace habilitada y con la fecha del reloj inyectado")
        void create_nace_habilitada() {
            BundleComponent component = BundleComponent.create(3L, 2L, 5, CatalogItemMother.RELOJ);

            assertThat(component.getId()).isNull();
            assertThat(component.isEnabled()).isTrue();
            assertThat(component.getQuantity()).isEqualTo(5);
            assertThat(component.getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        }

        @Test
        @DisplayName("un paquete no puede contenerse a sí mismo")
        void rechaza_el_paquete_dentro_de_si_mismo() {
            assertThatThrownBy(() -> BundleComponent.create(3L, 3L, 1, CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot contain itself: 3");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -100})
        @DisplayName("rechaza la cantidad que no sea mayor que cero")
        void rechaza_cantidad_no_positiva(int quantity) {
            assertThatThrownBy(
                    () -> BundleComponent.create(3L, 2L, quantity, CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be greater than zero");
        }

        @Test
        @DisplayName("rechaza cualquiera de los dos artículos nulo")
        void rechaza_articulos_nulos() {
            assertThatThrownBy(() -> BundleComponent.create(null, 2L, 1, CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bundleItemId is required");
            assertThatThrownBy(() -> BundleComponent.create(3L, null, 1, CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("componentItemId is required");
        }
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("changeQuantity es lo único editable de un componente")
        void change_quantity_cambia_la_cantidad() {
            BundleComponent component = CatalogItemMother.componente(9L, 3L, 2L, 1);

            component.changeQuantity(4);

            assertThat(component.getQuantity()).isEqualTo(4);
            assertThat(component.getBundleItemId()).isEqualTo(3L);
            assertThat(component.getComponentItemId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("changeQuantity aplica la misma invariante que el constructor")
        void change_quantity_aplica_la_misma_invariante() {
            BundleComponent component = CatalogItemMother.componente(9L, 3L, 2L, 1);

            assertThatThrownBy(() -> component.changeQuantity(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("greater than zero");

            assertThat(component.getQuantity()).isEqualTo(1);
        }
    }
}
