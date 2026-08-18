package com.vetsoftware.app.consultationtype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import com.vetsoftware.app.consultationtype.testsupport.ConsultationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConsultationTypeDto.from")
class ConsultationTypeDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        ConsultationType tipo = ConsultationTypeMother.consultaGeneral();

        ConsultationTypeDto dto = ConsultationTypeDto.from(tipo);

        assertThat(dto.id()).isEqualTo(ConsultationTypeMother.ID);
        assertThat(dto.name()).isEqualTo(ConsultationTypeMother.NOMBRE);
        assertThat(dto.description()).isEqualTo(ConsultationTypeMother.DESCRIPCION);
        assertThat(dto.createdDate()).isEqualTo(ConsultationTypeMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("propaga el tipo deshabilitado")
    void propaga_el_tipo_deshabilitado() {
        ConsultationTypeDto dto = ConsultationTypeDto.from(ConsultationTypeMother.deshabilitada());

        assertThat(dto.enabled()).isFalse();
    }
}
