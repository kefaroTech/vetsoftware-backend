package com.vetsoftware.app.prescription.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaEntity;
import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.domain.MedicamentRef;
import com.vetsoftware.app.prescription.testsupport.PrescriptionMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaMedicamentQueryPort (prescription)")
class JpaMedicamentQueryPortTest {

    @Mock
    private MedicamentPrescriptionJpaRepository repository;
    @Mock
    private MedicamentPrescriptionJpaEntity lineEntity;

    @InjectMocks
    private JpaMedicamentQueryPort port;

    @Test
    @DisplayName("findByPrescriptionId mapea cada linea a un MedicamentRef")
    void find_by_prescription_id_mapea_cada_linea() {
        when(repository.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                .thenReturn(List.of(lineEntity));
        when(lineEntity.getId()).thenReturn(PrescriptionMother.MEDICAMENT_ID);
        when(lineEntity.getName()).thenReturn("Amoxicilina 500mg");
        when(lineEntity.getPresentation()).thenReturn("Tableta");
        when(lineEntity.getQuantity()).thenReturn(2.0);
        when(lineEntity.getPosology()).thenReturn("Cada 12 horas por 7 dias");
        when(lineEntity.getObservation()).thenReturn("Con alimento");

        List<MedicamentRef> result = port.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID);

        assertThat(result).containsExactly(PrescriptionMother.MEDICAMENTO);
    }

    @Test
    @DisplayName("sin lineas devuelve una lista vacia")
    void sin_lineas_devuelve_lista_vacia() {
        when(repository.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID))
                .thenReturn(List.of());

        assertThat(port.findByPrescriptionId(PrescriptionMother.PRESCRIPTION_ID)).isEmpty();
    }
}
