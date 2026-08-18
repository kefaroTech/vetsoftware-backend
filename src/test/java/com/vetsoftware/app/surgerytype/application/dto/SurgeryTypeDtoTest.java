package com.vetsoftware.app.surgerytype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import com.vetsoftware.app.surgerytype.testsupport.SurgeryTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SurgeryTypeDto")
class SurgeryTypeDtoTest {

    @Nested
    @DisplayName("from")
    class From {

        @Test
        @DisplayName("copia cada campo del tipo de cirugia, incluida la empresa")
        void copia_cada_campo_del_tipo_de_cirugia() {
            SurgeryType tipo = SurgeryTypeMother.propioDeEmpresa();

            SurgeryTypeDto dto = SurgeryTypeDto.from(tipo);

            assertThat(dto.id()).isEqualTo(tipo.getId());
            assertThat(dto.name()).isEqualTo(tipo.getName());
            assertThat(dto.description()).isEqualTo(tipo.getDescription());
            assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(SurgeryTypeMother.EMPRESA));
            assertThat(dto.general()).isFalse();
            assertThat(dto.createdDate()).isEqualTo(tipo.getCreatedDate());
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("un tipo general no trae empresa: company es null")
        void un_tipo_general_no_trae_empresa() {
            SurgeryTypeDto dto = SurgeryTypeDto.from(SurgeryTypeMother.general());

            assertThat(dto.company()).isNull();
            assertThat(dto.general()).isTrue();
        }
    }
}
