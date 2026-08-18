package com.vetsoftware.app.prescription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.out.MedicamentQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindPrescriptionService")
class FindPrescriptionServiceTest {

    @Mock
    private PrescriptionRepository repository;
    @Mock
    private MedicamentQueryPort medicamentQueryPort;

    @InjectMocks
    private FindPrescriptionService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el DTO con los medicamentos resueltos por el puerto")
        void devuelve_el_dto_con_los_medicamentos() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .thenReturn(Optional.of(PrescriptionMother.persistida()));
            when(medicamentQueryPort.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(PrescriptionMother.unMedicamento());

            PrescriptionDto dto = service.findById(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID);

            assertThat(dto.medicaments()).containsExactly(PrescriptionMother.MEDICAMENTO);
        }
    }

    @Nested
    @DisplayName("receta inexistente")
    class Inexistente {

        @Test
        @DisplayName("no consulta medicamentos si la receta no existe para esa empresa")
        void no_consulta_medicamentos_si_no_existe() {
            when(repository.findByIdAndCompanyId(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(PrescriptionMother.PRESCRIPTION_ID,
                    PrescriptionMother.COMPANY_ID))
                    .isInstanceOf(PrescriptionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PrescriptionMother.PRESCRIPTION_ID));

            verifyNoInteractions(medicamentQueryPort);
        }
    }
}
