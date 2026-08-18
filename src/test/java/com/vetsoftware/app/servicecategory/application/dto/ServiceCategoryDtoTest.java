package com.vetsoftware.app.servicecategory.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.servicecategory.domain.ServiceCategory;
import com.vetsoftware.app.servicecategory.testsupport.ServiceCategoryMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceCategoryDto.from — mapeo campo por campo")
class ServiceCategoryDtoTest {

    @Test
    @DisplayName("copia cada campo de la categoria activa")
    void copia_cada_campo_de_la_categoria_activa() {
        ServiceCategory categoria = ServiceCategoryMother.activa();

        ServiceCategoryDto dto = ServiceCategoryDto.from(categoria);

        assertThat(dto.id()).isEqualTo(categoria.getId());
        assertThat(dto.name()).isEqualTo(categoria.getName());
        assertThat(dto.description()).isEqualTo(categoria.getDescription());
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(categoria.getCompany()));
        assertThat(dto.createdDate()).isEqualTo(categoria.getCreatedDate());
        assertThat(dto.updatedDate()).isEqualTo(categoria.getUpdatedDate());
        assertThat(dto.updatedBy()).isEqualTo(categoria.getUpdatedBy());
        assertThat(dto.version()).isEqualTo(categoria.getVersion());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("una categoria deshabilitada mapea enabled en falso")
    void una_categoria_deshabilitada_mapea_enabled_en_falso() {
        ServiceCategory categoria = ServiceCategoryMother.deshabilitada();

        ServiceCategoryDto dto = ServiceCategoryDto.from(categoria);

        assertThat(dto.enabled()).isFalse();
    }
}
