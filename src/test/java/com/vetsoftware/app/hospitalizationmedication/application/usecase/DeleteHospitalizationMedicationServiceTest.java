package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteHospitalizationMedicationService")
class DeleteHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;

    @InjectMocks
    private DeleteHospitalizationMedicationService service;

    @Nested
    @DisplayName("borrado")
    class Borrado {

        @Test
        @DisplayName("borra la orden encontrada")
        void borra_la_orden_encontrada() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID))
                    .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));

            service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID);

            verify(repository).delete(HospitalizationMedicationMother.MEDICATION_ID);
        }
    }

    @Nested
    @DisplayName("orden inexistente")
    class OrdenInexistente {

        @Test
        @DisplayName("no borra nada")
        void no_borra_nada() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.COMPANY_ID))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verify(repository, never()).delete(anyLong());
        }
    }

    /**
     * Antes de BE-COV el puerto no recibia companyId y el servicio comprobaba la
     * existencia con {@code findById(id)}: cualquier empleado con
     * {@code hospitalization.delete} borraba la orden de otra empresa adivinando el
     * id.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una orden de otra empresa no se borra: 404 y el repositorio no escribe")
        void una_orden_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.OTRA_COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                    HospitalizationMedicationMother.OTRA_COMPANY_ID))
                    .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                    .hasMessageContaining("HospitalizationMedication not found: "
                            + HospitalizationMedicationMother.MEDICATION_ID);

            verify(repository, never()).delete(anyLong());
        }
    }
}
