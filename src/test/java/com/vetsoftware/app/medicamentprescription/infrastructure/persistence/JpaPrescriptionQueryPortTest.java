package com.vetsoftware.app.medicamentprescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.domain.PrescriptionRef;
import com.vetsoftware.app.medicamentprescription.testsupport.MedicamentPrescriptionMother;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaEntity;
import com.vetsoftware.app.prescription.infrastructure.persistence.PrescriptionJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaPrescriptionQueryPort (medicamentprescription)")
class JpaPrescriptionQueryPortTest {

    @Mock
    private PrescriptionJpaRepository prescriptionJpaRepository;
    @Mock
    private PrescriptionJpaEntity entity;

    @InjectMocks
    private JpaPrescriptionQueryPort port;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("mapea la entidad encontrada a PrescriptionRef")
        void mapea_la_entidad_encontrada() {
            when(prescriptionJpaRepository.findById(MedicamentPrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.of(entity));
            when(entity.getId()).thenReturn(MedicamentPrescriptionMother.PRESCRIPTION_ID);
            when(entity.getDate()).thenReturn(MedicamentPrescriptionMother.FECHA);

            Optional<PrescriptionRef> result = port
                    .findById(MedicamentPrescriptionMother.PRESCRIPTION_ID);

            assertThat(result).contains(MedicamentPrescriptionMother.RECETA);
        }

        @Test
        @DisplayName("una receta inexistente devuelve vacio")
        void receta_inexistente_devuelve_vacio() {
            when(prescriptionJpaRepository.findById(MedicamentPrescriptionMother.PRESCRIPTION_ID))
                    .thenReturn(Optional.empty());

            assertThat(port.findById(MedicamentPrescriptionMother.PRESCRIPTION_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByIdAndCompanyId")
    class FindByIdAndCompanyId {

        @Test
        @DisplayName("mapea la entidad de la empresa a PrescriptionRef")
        void mapea_la_entidad_de_la_empresa() {
            when(prescriptionJpaRepository.findByIdAndCompany_Id(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.of(entity));
            when(entity.getId()).thenReturn(MedicamentPrescriptionMother.PRESCRIPTION_ID);
            when(entity.getDate()).thenReturn(MedicamentPrescriptionMother.FECHA);

            Optional<PrescriptionRef> result = port.findByIdAndCompanyId(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID);

            assertThat(result).contains(MedicamentPrescriptionMother.RECETA);
        }

        @Test
        @DisplayName("una receta de otra empresa devuelve vacio")
        void receta_de_otra_empresa_devuelve_vacio() {
            when(prescriptionJpaRepository.findByIdAndCompany_Id(
                    MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThat(port.findByIdAndCompanyId(MedicamentPrescriptionMother.PRESCRIPTION_ID,
                    MedicamentPrescriptionMother.COMPANY_ID)).isEmpty();
        }
    }
}
