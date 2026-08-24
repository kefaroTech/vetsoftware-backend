package com.vetsoftware.app.catalogitem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("CatalogItemDependency — invariantes de una regla del configurador")
class CatalogItemDependencyTest {

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("create nace habilitada y con la fecha del reloj inyectado")
        void create_nace_habilitada() {
            CatalogItemDependency dependency = CatalogItemDependency.create(1L, 2L,
                    RelationType.REQUIRES, "Necesitas caja", CatalogItemMother.RELOJ);

            assertThat(dependency.getId()).isNull();
            assertThat(dependency.isEnabled()).isTrue();
            assertThat(dependency.getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
            assertThat(dependency.getRelationType()).isEqualTo(RelationType.REQUIRES);
        }

        @Test
        @DisplayName("cierra el ciclo trivial: un artículo no puede depender de sí mismo")
        void rechaza_la_dependencia_a_si_misma() {
            assertThatThrownBy(() -> CatalogItemDependency.create(7L, 7L, RelationType.REQUIRES,
                    null, CatalogItemMother.RELOJ)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot depend on itself: 7");
        }

        @Test
        @DisplayName("rechaza cualquiera de los dos artículos nulo")
        void rechaza_articulos_nulos() {
            assertThatThrownBy(() -> CatalogItemDependency.create(null, 2L, RelationType.REQUIRES,
                    null, CatalogItemMother.RELOJ)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("catalogItemId is required");
            assertThatThrownBy(() -> CatalogItemDependency.create(1L, null, RelationType.REQUIRES,
                    null, CatalogItemMother.RELOJ)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("relatedItemId is required");
        }

        @Test
        @DisplayName("rechaza el tipo de relación nulo")
        void rechaza_tipo_de_relacion_nulo() {
            assertThatThrownBy(
                    () -> CatalogItemDependency.create(1L, 2L, null, null, CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("relationType is required");
        }

        @Test
        @DisplayName("rechaza la nota de más de 255 caracteres, que es el ancho de la columna")
        void rechaza_nota_demasiado_larga() {
            assertThatThrownBy(() -> CatalogItemDependency.create(1L, 2L, RelationType.REQUIRES,
                    "N".repeat(256), CatalogItemMother.RELOJ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("255 chars or less");
        }

        @Test
        @DisplayName("acepta la nota ausente: el mensaje al cliente es opcional")
        void acepta_nota_ausente() {
            assertThat(CatalogItemDependency
                    .create(1L, 2L, RelationType.RECOMMENDS, null, CatalogItemMother.RELOJ)
                    .getNote()).isNull();
        }
    }

    @Nested
    @DisplayName("Arrastre")
    class Arrastre {

        @ParameterizedTest
        @EnumSource(RelationType.class)
        @DisplayName("solo REQUIRES arrastra, y por eso solo él puede formar ciclos")
        void solo_requires_arrastra(RelationType relationType) {
            CatalogItemDependency dependency = CatalogItemDependency.create(1L, 2L, relationType,
                    null, CatalogItemMother.RELOJ);

            assertThat(dependency.arrastra()).isEqualTo(relationType == RelationType.REQUIRES);
        }
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("update cambia el sentido y la nota, nunca el par de artículos")
        void update_cambia_el_sentido_y_la_nota() {
            CatalogItemDependency dependency = CatalogItemMother.dependencia(9L, 1L, 2L,
                    RelationType.RECOMMENDS);

            dependency.update(RelationType.REQUIRES, "Ahora es obligatorio");

            assertThat(dependency.getRelationType()).isEqualTo(RelationType.REQUIRES);
            assertThat(dependency.getNote()).isEqualTo("Ahora es obligatorio");
            assertThat(dependency.getCatalogItemId()).isEqualTo(1L);
            assertThat(dependency.getRelatedItemId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("update aplica las mismas invariantes que el constructor")
        void update_aplica_las_mismas_invariantes() {
            CatalogItemDependency dependency = CatalogItemMother.dependencia(9L, 1L, 2L,
                    RelationType.REQUIRES);

            assertThatThrownBy(() -> dependency.update(null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("relationType is required");
        }
    }
}
