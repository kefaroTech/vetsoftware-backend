package com.vetsoftware.app.deworming.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.domain.DewormingType;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("DewormingDto.from")
class DewormingDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado y aplana las refs en summaries")
    void copia_cada_campo_y_aplana_las_refs() {
        Deworming deworming = DewormingMother.desparasitacionValida();

        DewormingDto dto = DewormingDto.from(deworming);

        assertThat(dto.id()).isEqualTo(DewormingMother.DEWORMING_ID);
        assertThat(dto.date()).isEqualTo(deworming.getDate());
        assertThat(dto.lastDeworming()).isEqualTo(deworming.getLastDeworming());
        assertThat(dto.type()).isEqualTo(DewormingType.INTERNAL);
        assertThat(dto.product()).isEqualTo("Drontal Plus");
        assertThat(dto.dosage()).isEqualTo("1 tableta / 10kg");
        assertThat(dto.nextControl()).isEqualTo(deworming.getNextControl());
        assertThat(dto.observations()).isEqualTo("Sin reacciones adversas");
        assertThat(dto.animal()).isEqualTo(new AnimalSummaryDto(DewormingMother.FIRULAIS.id(),
                DewormingMother.FIRULAIS.name(), DewormingMother.FIRULAIS.code()));
        assertThat(dto.consultation()).isEqualTo(new ConsultationSummaryDto(
                DewormingMother.CONSULTA.id(), DewormingMother.CONSULTA.date()));
        assertThat(dto.company()).isEqualTo(new CompanySummaryDto(DewormingMother.CLINICA.id(),
                DewormingMother.CLINICA.name(), DewormingMother.CLINICA.identifier()));
        assertThat(dto.createdDate()).isEqualTo(DewormingMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("sin consulta asociada, consultation es null")
    void sin_consulta_asociada_consultation_es_null() {
        DewormingDto dto = DewormingDto.from(DewormingMother.sinConsulta());

        assertThat(dto.consultation()).isNull();
        // el resto del agregado si viaja: la ausencia de consulta no vacia el DTO
        // entero.
        assertThat(dto.product()).isEqualTo("Frontline");
        assertThat(dto.animal()).isNotNull();
        assertThat(dto.company()).isNotNull();
    }

    @Test
    @DisplayName("propaga la desparasitacion deshabilitada")
    void propaga_la_desparasitacion_deshabilitada() {
        assertThat(DewormingDto.from(DewormingMother.deshabilitada()).enabled()).isFalse();
    }
}
