package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * No aparecia en la worklist de JaCoCo del dia pero es la unica clase de
 * application/usecase de esta feature que se quedaba sin ningun test: se cubre
 * igual, con el mismo nivel de exigencia que el resto de la feature (patron
 * aplicado tambien en hospitalizationprocedure).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FindHospitalizationMedicationService")
class FindHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;

    @InjectMocks
    private FindHospitalizationMedicationService service;

    @Test
    @DisplayName("devuelve el DTO de la orden acotada por empresa")
    void devuelve_el_dto_de_la_orden_acotada_por_empresa() {
        when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));

        HospitalizationMedicationDto dto = service.findById(
                HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
        assertThat(dto.name()).isEqualTo("Amoxicilina 500mg");
    }

    @Test
    @DisplayName("una orden de otra empresa no se encuentra")
    void una_orden_de_otra_empresa_no_se_encuentra() {
        when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                .hasMessageContaining("HospitalizationMedication not found: "
                        + HospitalizationMedicationMother.MEDICATION_ID);
    }
}
