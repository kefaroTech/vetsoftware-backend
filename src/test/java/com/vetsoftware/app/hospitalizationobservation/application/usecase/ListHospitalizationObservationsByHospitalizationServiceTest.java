package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.testsupport.HospitalizationObservationMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationObservationsByHospitalizationService")
class ListHospitalizationObservationsByHospitalizationServiceTest {

    @Mock
    private HospitalizationObservationRepository repository;

    private ListHospitalizationObservationsByHospitalizationService service;

    @BeforeEach
    void crearServicio() {
        service = new ListHospitalizationObservationsByHospitalizationService(repository);
    }

    @Test
    @DisplayName("delega la paginacion en el repositorio y mapea el contenido a dto")
    void delega_la_paginacion_y_mapea_el_contenido() {
        HospitalizationObservation observacion = HospitalizationObservationMother
                .observacionValida();
        PageResult<HospitalizationObservation> pagina = new PageResult<>(List.of(observacion), 0,
                20, 1, 1);
        when(repository.findAllByHospitalizationIdAndCompanyId(
                HospitalizationObservationMother.HOSPITALIZATION_ID,
                HospitalizationObservationMother.COMPANY_ID, 0, 20)).thenReturn(pagina);

        PageResult<HospitalizationObservationDto> resultado = service.listByHospitalization(
                HospitalizationObservationMother.HOSPITALIZATION_ID,
                HospitalizationObservationMother.COMPANY_ID, 0, 20);

        assertThat(resultado.content()).extracting(HospitalizationObservationDto::id)
                .containsExactly(HospitalizationObservationMother.OBSERVATION_ID);
        verify(repository).findAllByHospitalizationIdAndCompanyId(
                HospitalizationObservationMother.HOSPITALIZATION_ID,
                HospitalizationObservationMother.COMPANY_ID, 0, 20);
    }

    @Test
    @DisplayName("una hospitalizacion sin observaciones devuelve una pagina vacia")
    void hospitalizacion_sin_observaciones_devuelve_pagina_vacia() {
        PageResult<HospitalizationObservation> vacia = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(repository.findAllByHospitalizationIdAndCompanyId(
                HospitalizationObservationMother.HOSPITALIZATION_ID,
                HospitalizationObservationMother.COMPANY_ID, 0, 20)).thenReturn(vacia);

        PageResult<HospitalizationObservationDto> resultado = service.listByHospitalization(
                HospitalizationObservationMother.HOSPITALIZATION_ID,
                HospitalizationObservationMother.COMPANY_ID, 0, 20);

        assertThat(resultado.content()).isEmpty();
    }
}
