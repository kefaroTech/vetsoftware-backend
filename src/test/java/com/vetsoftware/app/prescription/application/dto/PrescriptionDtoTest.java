package com.vetsoftware.app.prescription.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PrescriptionDto")
class PrescriptionDtoTest {

    @Test
    @DisplayName("from(Prescription) sin medicamentos deja la lista vacia")
    void from_sin_medicamentos_deja_la_lista_vacia() {
        PrescriptionDto dto = PrescriptionDto.from(PrescriptionMother.persistida());

        assertThat(dto.id()).isEqualTo(PrescriptionMother.PRESCRIPTION_ID);
        assertThat(dto.date()).isEqualTo(PrescriptionMother.FECHA);
        assertThat(dto.diagnosis()).isEqualTo("Otitis externa");
        assertThat(dto.observations()).isEqualTo("Control en 7 dias");
        assertThat(dto.animal()).isEqualTo(AnimalSummaryDto.from(PrescriptionMother.MASCOTA));
        assertThat(dto.consultation())
                .isEqualTo(ConsultationSummaryDto.from(PrescriptionMother.CONSULTA));
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(PrescriptionMother.CLINICA));
        assertThat(dto.medicaments()).isEmpty();
        assertThat(dto.createdDate()).isEqualTo(PrescriptionMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from(Prescription, medicaments) conserva la lista recibida")
    void from_con_medicamentos_conserva_la_lista() {
        List<com.vetsoftware.app.prescription.domain.MedicamentRef> medicaments = PrescriptionMother
                .unMedicamento();

        PrescriptionDto dto = PrescriptionDto.from(PrescriptionMother.persistida(), medicaments);

        assertThat(dto.medicaments()).isEqualTo(medicaments);
    }
}
