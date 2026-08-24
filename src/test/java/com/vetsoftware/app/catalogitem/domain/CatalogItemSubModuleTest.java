package com.vetsoftware.app.catalogitem.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CatalogItemSubModule y SubModuleRef — el puente entre vender y funcionar")
class CatalogItemSubModuleTest {

    @Test
    @DisplayName("create nace habilitado y con la fecha del reloj inyectado")
    void create_nace_habilitado() {
        CatalogItemSubModule link = CatalogItemSubModule.create(1L, CatalogItemMother.consultas(),
                CatalogItemMother.RELOJ);

        assertThat(link.getId()).isNull();
        assertThat(link.isEnabled()).isTrue();
        assertThat(link.getCatalogItemId()).isEqualTo(1L);
        assertThat(link.getSubModule().code()).isEqualTo("CONSULTATIONS");
        assertThat(link.getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
    }

    @Test
    @DisplayName("rechaza el artículo o el submódulo ausentes")
    void rechaza_extremos_ausentes() {
        assertThatThrownBy(() -> CatalogItemSubModule.create(null, CatalogItemMother.consultas(),
                CatalogItemMother.RELOJ)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogItemId is required");
        assertThatThrownBy(() -> CatalogItemSubModule.create(1L, null, CatalogItemMother.RELOJ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subModule is required");
    }

    @Test
    @DisplayName("enable y disable mueven el borrado lógico")
    void enable_y_disable() {
        CatalogItemSubModule link = CatalogItemMother.vinculo(5L, 1L);

        link.disable();
        assertThat(link.isEnabled()).isFalse();

        link.enable();
        assertThat(link.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("el companion VO exige sus tres campos: un ref a medias no vale para nada")
    void el_companion_vo_exige_sus_tres_campos() {
        assertThatThrownBy(() -> new SubModuleRef(null, "Consultas", "CONSULTATIONS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub module id is required");
        assertThatThrownBy(() -> new SubModuleRef(1L, "  ", "CONSULTATIONS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub module name is required");
        assertThatThrownBy(() -> new SubModuleRef(1L, "Consultas", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sub module code is required");
    }

    @Test
    @DisplayName("un arco del grafo exige sus dos extremos")
    void el_arco_exige_sus_dos_extremos() {
        assertThatThrownBy(() -> new DependencyEdge(null, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("catalogItemId is required");
        assertThatThrownBy(() -> new DependencyEdge(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatedItemId is required");
    }
}
