package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaEntity;
import com.vetsoftware.app.medicament.infrastructure.persistence.MedicamentJpaRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentRef;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMedicamentQueryPort (medicamentprescription)")
class JpaMedicamentQueryPortTest {

    @Mock
    private MedicamentJpaRepository medicamentJpaRepository;
    @Mock
    private MedicamentJpaEntity entity;

    @InjectMocks
    private JpaMedicamentQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la entidad encontrada a MedicamentRef")
        void mapea_la_entidad_encontrada() {
            when(medicamentJpaRepository.findById(MedicamentPrescriptionMother.MEDICAMENT_ID))
                    .thenReturn(Optional.of(entity));
            when(entity.getId()).thenReturn(MedicamentPrescriptionMother.MEDICAMENT_ID);
            when(entity.getName()).thenReturn(MedicamentPrescriptionMother.MEDICAMENTO.name());

            Optional<MedicamentRef> result = port
                    .findById(MedicamentPrescriptionMother.MEDICAMENT_ID);

            assertThat(result).contains(MedicamentPrescriptionMother.MEDICAMENTO);
        }

        @Test
        @DisplayName("un medicamento inexistente devuelve vacio")
        void medicamento_inexistente_devuelve_vacio() {
            when(medicamentJpaRepository.findById(MedicamentPrescriptionMother.MEDICAMENT_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(MedicamentPrescriptionMother.MEDICAMENT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAvailableById")
    class FindAvailableById {

        @Test
        @DisplayName("mapea la entidad disponible para la empresa a MedicamentRef")
        void mapea_la_entidad_disponible() {
            when(medicamentJpaRepository.findAvailableById(
                    MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(entity));
            when(entity.getId()).thenReturn(MedicamentPrescriptionMother.MEDICAMENT_ID);
            when(entity.getName()).thenReturn(MedicamentPrescriptionMother.MEDICAMENTO.name());

            Optional<MedicamentRef> result = port.findAvailableById(
                    MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID);

            assertThat(result).contains(MedicamentPrescriptionMother.MEDICAMENTO);
        }

        @Test
        @DisplayName("no disponible para la empresa devuelve vacio")
        void no_disponible_devuelve_vacio() {
            when(medicamentJpaRepository.findAvailableById(
                    MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(port.findAvailableById(MedicamentPrescriptionMother.MEDICAMENT_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).isEmpty();
        }
    }
}
