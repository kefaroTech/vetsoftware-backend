package com.vetsoftware.app.spa.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SpaDto.from")
class SpaDtoTest {

    @Test
    @DisplayName("mapea cada campo del agregado, campo por campo")
    void mapea_cada_campo() {
        Spa spa = SpaMother.spaValido();

        SpaDto dto = SpaDto.from(spa);

        assertThat(dto.id()).isEqualTo(spa.getId());
        assertThat(dto.date()).isEqualTo(spa.getDate());
        assertThat(dto.spaType()).isEqualTo(SpaTypeSummaryDto.from(spa.getSpaType()));
        assertThat(dto.reason()).isEqualTo(spa.getReason());
        assertThat(dto.details()).isEqualTo(spa.getDetails());
        assertThat(dto.observations()).isEqualTo(spa.getObservations());
        assertThat(dto.status()).isEqualTo("AGENDADA");
        assertThat(dto.animal()).isEqualTo(AnimalSummaryDto.from(spa.getAnimal()));
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(spa.getCompany()));
        assertThat(dto.createdDate()).isEqualTo(spa.getCreatedDate());
        assertThat(dto.enabled()).isEqualTo(spa.isEnabled());
    }

    @Test
    @DisplayName("mapea el status como el nombre del enum")
    void mapea_el_status_como_el_nombre_del_enum() {
        Spa spa = SpaMother.spaValido();
        spa.changeStatus(SpaStatus.COMPLETADO);

        assertThat(SpaDto.from(spa).status()).isEqualTo("COMPLETADO");
    }

    @Test
    @DisplayName("un spa deshabilitado mapea enabled en false")
    void un_spa_deshabilitado_mapea_enabled_en_false() {
        Spa spa = SpaMother.spaValido();
        spa.disable();

        assertThat(SpaDto.from(spa).enabled()).isFalse();
    }
}
