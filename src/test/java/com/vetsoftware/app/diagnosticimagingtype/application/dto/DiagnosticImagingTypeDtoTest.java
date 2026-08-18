package com.vetsoftware.app.diagnosticimagingtype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import com.vetsoftware.app.diagnosticimagingtype.testsupport.DiagnosticImagingTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DiagnosticImagingTypeDto")
class DiagnosticImagingTypeDtoTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("copia cada campo de un tipo propio de empresa")
        void copia_cada_campo_de_un_tipo_propio_de_empresa() {
            DiagnosticImagingType type = DiagnosticImagingTypeMother.propiaDeEmpresa();

            DiagnosticImagingTypeDto dto = DiagnosticImagingTypeDto.from(type);

            assertThat(dto.id()).isEqualTo(type.getId());
            assertThat(dto.name()).isEqualTo(type.getName());
            assertThat(dto.description()).isEqualTo(type.getDescription());
            assertThat(dto.company().id()).isEqualTo(type.getCompany().id());
            assertThat(dto.general()).isEqualTo(type.isGeneral());
            assertThat(dto.createdDate()).isEqualTo(type.getCreatedDate());
            assertThat(dto.enabled()).isEqualTo(type.isEnabled());
        }

        @Test
        @DisplayName("un tipo general sin company deja el companion en null")
        void un_tipo_general_deja_el_companion_en_null() {
            DiagnosticImagingType type = DiagnosticImagingTypeMother.general();

            DiagnosticImagingTypeDto dto = DiagnosticImagingTypeDto.from(type);

            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
        }
    }
}
