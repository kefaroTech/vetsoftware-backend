package com.vetsoftware.app.vaccination.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccination.testsupport.VaccinationMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationDto")
class VaccinationDtoTest {

    @Test
    @DisplayName("from() copia cada campo y mapea cada companion VO a su summary")
    void from_copia_cada_campo_y_mapea_cada_companion() {
        VaccinationDto dto = VaccinationDto.from(VaccinationMother.vigente());

        assertThat(dto.id()).isEqualTo(VaccinationMother.VACCINATION_ID);
        assertThat(dto.date()).isEqualTo(VaccinationMother.FECHA);
        assertThat(dto.vaccinationType())
                .isEqualTo(VaccinationTypeSummaryDto.from(VaccinationMother.RABIA));
        assertThat(dto.lot()).isEqualTo("L-2026-A");
        assertThat(dto.notes()).isEqualTo("Sin reaccion");
        assertThat(dto.route()).isEqualTo("Subcutanea");
        assertThat(dto.applicationSite()).isEqualTo("Cuello");
        assertThat(dto.nextVaccination()).isEqualTo(VaccinationMother.PROXIMA);
        assertThat(dto.animal()).isEqualTo(AnimalSummaryDto.from(VaccinationMother.FIRULAIS));
        assertThat(dto.consultation())
                .isEqualTo(ConsultationSummaryDto.from(VaccinationMother.CONSULTA));
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(VaccinationMother.CLINICA));
        assertThat(dto.createdDate()).isEqualTo(VaccinationMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() deja la consulta en null cuando la vacuna no tiene consulta asociada")
    void from_deja_la_consulta_en_null_sin_consulta_asociada() {
        VaccinationDto dto = VaccinationDto.from(VaccinationMother.sinConsulta());

        assertThat(dto.consultation()).isNull();
    }

    @Test
    @DisplayName("from() refleja una vacuna deshabilitada")
    void from_refleja_una_vacuna_deshabilitada() {
        VaccinationDto dto = VaccinationDto.from(VaccinationMother.deshabilitada());

        assertThat(dto.enabled()).isFalse();
    }
}
