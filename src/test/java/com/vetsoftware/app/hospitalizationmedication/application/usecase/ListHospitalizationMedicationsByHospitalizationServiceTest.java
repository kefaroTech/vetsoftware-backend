package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.testsupport.HospitalizationMedicationMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationMedicationsByHospitalizationService")
class ListHospitalizationMedicationsByHospitalizationServiceTest {

    @Mock
    private HospitalizationMedicationRepository repository;

    @InjectMocks
    private ListHospitalizationMedicationsByHospitalizationService service;

    @Test
    @DisplayName("mapea cada orden a DTO conservando el orden del repositorio")
    void mapea_cada_orden_a_dto_conservando_el_orden() {
        when(repository.findAllByHospitalizationIdAndCompanyId(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(HospitalizationMedicationMother.activo(7L),
                        HospitalizationMedicationMother.activo(4L)), 0, 20, 2L, 1));

        PageResult<HospitalizationMedicationDto> resultado = service.listByHospitalization(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 0, 20);

        assertThat(resultado.content()).extracting(HospitalizationMedicationDto::id)
                .containsExactly(7L, 4L);
        assertThat(resultado.totalElements()).isEqualTo(2L);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("una hospitalizacion sin ordenes devuelve una pagina vacia, no null")
    void una_hospitalizacion_sin_ordenes_devuelve_pagina_vacia() {
        when(repository.findAllByHospitalizationIdAndCompanyId(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 0, 20))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        PageResult<HospitalizationMedicationDto> resultado = service.listByHospitalization(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 0, 20);

        assertThat(resultado.content()).isEmpty();
    }

    @Test
    @DisplayName("consulta siempre acotando por hospitalizacion y empresa")
    void consulta_siempre_acotando_por_hospitalizacion_y_empresa() {
        when(repository.findAllByHospitalizationIdAndCompanyId(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 1, 5))
                .thenReturn(new PageResult<>(List.of(), 1, 5, 0L, 0));

        service.listByHospitalization(HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 1, 5);

        verify(repository).findAllByHospitalizationIdAndCompanyId(
                HospitalizationMedicationMother.HOSPITALIZATION_ID,
                HospitalizationMedicationMother.COMPANY_ID, 1, 5);
    }
}
