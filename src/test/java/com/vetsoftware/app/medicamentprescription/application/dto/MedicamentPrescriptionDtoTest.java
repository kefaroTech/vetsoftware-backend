package com.vetsoftware.app.medicamentprescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescription;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo compila, pasa cualquier test de "no es null", y solo se
 * ve en pantalla.
 */
@DisplayName("MedicamentPrescriptionDto.from")
class MedicamentPrescriptionDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        MedicamentPrescription line = MedicamentPrescriptionMother.persistida();

        MedicamentPrescriptionDto dto = MedicamentPrescriptionDto.from(line);

        assertThat(dto.id()).isEqualTo(MedicamentPrescriptionMother.ID);
        assertThat(dto.medicamentId()).isEqualTo(MedicamentPrescriptionMother.MEDICAMENT_ID);
        assertThat(dto.name()).isEqualTo(MedicamentPrescriptionMother.MEDICAMENTO.name());
        assertThat(dto.presentation()).isEqualTo("Tableta");
        assertThat(dto.quantity()).isEqualTo(2.0);
        assertThat(dto.posology()).isEqualTo("Cada 12 horas por 7 dias");
        assertThat(dto.observation()).isEqualTo("Con alimento");
        assertThat(dto.createdDate()).isEqualTo(MedicamentPrescriptionMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana el companion VO de la receta en su summary")
    void aplana_el_companion_vo_de_la_receta() {
        MedicamentPrescriptionDto dto = MedicamentPrescriptionDto
                .from(MedicamentPrescriptionMother.persistida());

        assertThat(dto.prescription()).isEqualTo(new PrescriptionSummaryDto(
                MedicamentPrescriptionMother.PRESCRIPTION_ID, MedicamentPrescriptionMother.FECHA));
    }

    @Test
    @DisplayName("propaga la linea deshabilitada")
    void propaga_la_linea_deshabilitada() {
        assertThat(MedicamentPrescriptionDto.from(MedicamentPrescriptionMother.deshabilitada())
                .enabled()).isFalse();
    }
}
