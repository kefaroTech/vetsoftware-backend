package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateHospitalizationMedicationService")
class ReactivateHospitalizationMedicationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;

    @InjectMocks
    private ReactivateHospitalizationMedicationService service;

    @Test
    @DisplayName("reactiva y devuelve la orden ya habilitada")
    void reactiva_y_devuelve_la_orden_ya_habilitada() {
        when(repository.reactivate(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMedicationMother.activo()));

        HospitalizationMedicationDto dto = service.execute(
                HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationMedicationMother.MEDICATION_ID);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("cero filas afectadas es no-encontrado y evita la lectura posterior")
    void cero_filas_afectadas_es_no_encontrado() {
        when(repository.reactivate(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                .hasMessageContaining("HospitalizationMedication not found: "
                        + HospitalizationMedicationMother.MEDICATION_ID);

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("si la orden desaparece entre el UPDATE y el SELECT, falla como no-encontrada")
    void si_la_orden_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationMedicationNotFoundException.class);
    }

    /**
     * Antes de BE-COV el UPDATE nativo era {@code WHERE id = :id} a secas: en
     * reactivacion no hay lectura previa que valide la propiedad, asi que el SQL
     * era la unica barrera y no existia. Ahora el {@code EXISTS} contra la
     * hospitalizacion padre acota por empresa y cero filas afectadas sale como 404.
     */
    @Test
    @DisplayName("una orden de otra empresa no se reactiva: 404 y no relee nada")
    void una_orden_de_otra_empresa_no_se_reactiva() {
        when(repository.reactivate(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.OTRA_COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(HospitalizationMedicationMother.MEDICATION_ID,
                HospitalizationMedicationMother.OTRA_COMPANY_ID))
                .isInstanceOf(HospitalizationMedicationNotFoundException.class)
                .hasMessageContaining("HospitalizationMedication not found: "
                        + HospitalizationMedicationMother.MEDICATION_ID);

        verify(repository, never()).findByIdAndCompanyId(anyLong(), anyLong());
    }
}
