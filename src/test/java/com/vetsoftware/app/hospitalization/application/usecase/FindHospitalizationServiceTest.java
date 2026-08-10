package com.vetsoftware.app.hospitalization.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
@DisplayName("FindHospitalizationService")
class FindHospitalizationServiceTest {

    private static final Long OTRA_COMPANY = 77L;

    @Mock
    private HospitalizationRepository repository;

    @InjectMocks
    private FindHospitalizationService service;

    @Test
    @DisplayName("devuelve el DTO completo de la hospitalizacion de la empresa del usuario")
    void devuelve_el_dto_completo() {
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.of(HospitalizationMother.internado()));

        HospitalizationDto dto = service.findById(HospitalizationMother.HOSPITALIZATION_ID,
                HospitalizationMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(HospitalizationMother.HOSPITALIZATION_ID);
        assertThat(dto.animal().name()).isEqualTo("Firulais");
        assertThat(dto.consultation().id()).isEqualTo(HospitalizationMother.CONSULTATION_ID);
        assertThat(dto.company().identifier()).isEqualTo("900123456");
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("id inexistente en la empresa: excepcion de dominio con el id")
    void id_inexistente_en_la_empresa() {
        when(repository.findByIdAndCompanyId(404L, HospitalizationMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(404L, HospitalizationMother.COMPANY_ID))
                .isInstanceOf(HospitalizationNotFoundException.class)
                .hasMessageContaining("Hospitalization not found: 404");
    }

    @Test
    @DisplayName("una hospitalizacion de otra empresa se comporta como inexistente")
    void hospitalizacion_de_otra_empresa_es_inexistente() {
        // El aislamiento entre tenants vive en la consulta acotada: si el service
        // usara findById a secas, este caso devolveria el recurso ajeno.
        when(repository.findByIdAndCompanyId(HospitalizationMother.HOSPITALIZATION_ID,
                OTRA_COMPANY)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.findById(HospitalizationMother.HOSPITALIZATION_ID, OTRA_COMPANY))
                .isInstanceOf(HospitalizationNotFoundException.class).hasMessageContaining(
                        "Hospitalization not found: " + HospitalizationMother.HOSPITALIZATION_ID);
    }
}
