package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMedicamentPrescriptionChildrenQueryPort (prescription)")
class JpaMedicamentPrescriptionChildrenQueryPortTest {

    @Mock
    private MedicamentPrescriptionJpaRepository jpaRepository;

    @InjectMocks
    private JpaMedicamentPrescriptionChildrenQueryPort port;

    @Test
    @DisplayName("existsActiveByPrescriptionId delega en existsByPrescription_Id")
    void delega_en_exists_by_prescription_id() {
        when(jpaRepository.existsByPrescription_Id(PrescriptionMother.PRESCRIPTION_ID))
                .thenReturn(true);

        assertThat(port.existsActiveByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID)).isTrue();
    }

    @Test
    @DisplayName("sin medicamentos activos devuelve false")
    void sin_medicamentos_activos_devuelve_false() {
        when(jpaRepository.existsByPrescription_Id(PrescriptionMother.PRESCRIPTION_ID))
                .thenReturn(false);

        assertThat(port.existsActiveByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID)).isFalse();
    }
}
