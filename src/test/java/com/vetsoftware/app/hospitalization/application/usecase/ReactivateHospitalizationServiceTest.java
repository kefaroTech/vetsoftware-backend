package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import com.vetsoftware.app.hospitalization.testsupport.HospitalizationMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateHospitalizationService")
class ReactivateHospitalizationServiceTest {

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private ReactivateHospitalizationService service;

    @Test
    @DisplayName("reactiva y devuelve el DTO releido de base")
    void reactiva_y_devuelve_el_dto_releido() {
        when(repository.reactivate(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.internado()));

        HospitalizationDto dto = service.execute(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("cero filas actualizadas: excepcion de dominio y no se relee")
    void cero_filas_actualizadas_no_relee() {
        when(repository.reactivate(404L, HospitalizationMother.COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(404L, HospitalizationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class)
                .hasMessageContaining("Hospitalization not found: 404");

        verify(repository, never()).findByIdAndCompanyId(any(), any());
    }

    @Test
    @DisplayName("reactivada pero desaparecida entre las dos consultas: excepcion de dominio")
    void reactivada_pero_desaparecida() {
        // Carrera real: otro proceso la borra entre el UPDATE y el SELECT.
        when(repository.reactivate(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class).hasMessageContaining(
                        "Hospitalization not found: " + HospitalizationMother.HOSPITALIZATION_ID);
    }

    /**
     * Antes de BE-COV el UPDATE nativo era {@code WHERE id = :id} a secas y el
     * puerto no recibia companyId: en reactivacion no hay lectura previa que valide
     * la propiedad, asi que el SQL era la unica barrera y no existia. Ahora el
     * filtro por empresa viaja hasta el UPDATE: cero filas afectadas es «no existe
     * en TU empresa» y sale como 404, sin releer nada.
     */
    @Test
    @DisplayName("una hospitalizacion de otra empresa no se reactiva: 404 y no relee nada")
    void una_hospitalizacion_de_otra_empresa_no_se_reactiva() {
        when(repository.reactivate(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.OTRA_COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.OTRA_COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class).hasMessageContaining(
                        "Hospitalization not found: " + HospitalizationMother.HOSPITALIZATION_ID);

        verify(repository, never()).findByIdAndCompanyId(any(), any());
    }
}
