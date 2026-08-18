package com.vetsoftware.app.medicament.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMedicamentPrescriptionChildrenQueryPort")
class JpaMedicamentPrescriptionChildrenQueryPortTest {

    @Mock
    private MedicamentPrescriptionJpaRepository jpaRepository;

    @InjectMocks
    private JpaMedicamentPrescriptionChildrenQueryPort port;

    @Test
    @DisplayName("delega en existsByMedicament_Id")
    void delega_en_exists_by_medicament_id() {
        when(jpaRepository.existsByMedicament_Id(1L)).thenReturn(true);

        assertThat(port.existsActiveByMedicamentId(1L)).isTrue();
    }

    @Test
    @DisplayName("devuelve false cuando no hay recetas activas")
    void devuelve_false_sin_recetas_activas() {
        when(jpaRepository.existsByMedicament_Id(1L)).thenReturn(false);

        assertThat(port.existsActiveByMedicamentId(1L)).isFalse();
    }
}
