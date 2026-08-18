package com.vetsoftware.app.clinicalhistory.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.clinicalhistory.application.dto.ClinicalEventDto;
import com.vetsoftware.app.clinicalhistory.application.port.out.ClinicalEventRepository;
import com.vetsoftware.app.clinicalhistory.application.query.GetClinicalHistoryQuery;
import com.vetsoftware.app.clinicalhistory.domain.ClinicalEvent;
import com.vetsoftware.app.clinicalhistory.testsupport.ClinicalHistoryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetClinicalHistoryService — delega en el repositorio paginado")
class GetClinicalHistoryServiceTest {

    @Mock
    private ClinicalEventRepository repository;

    private GetClinicalHistoryService service;

    @org.junit.jupiter.api.BeforeEach
    void construirServicio() {
        service = new GetClinicalHistoryService(repository);
    }

    @Test
    @DisplayName("mapea el contenido de la página conservando sus metadatos")
    void mapea_el_contenido_conservando_los_metadatos() {
        GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
        ClinicalEvent evento = ClinicalHistoryMother.consulta();
        PageResult<ClinicalEvent> pagina = new PageResult<>(List.of(evento), 0, 20, 1L, 1);
        when(repository.findHistoryPage(query, 0, 20)).thenReturn(pagina);

        PageResult<ClinicalEventDto> resultado = service.execute(query, 0, 20);

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().getFirst().sourceId()).isEqualTo(evento.sourceId());
        assertThat(resultado.page()).isEqualTo(0);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(1L);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("pasa page y pageSize tal cual al repositorio")
    void pasa_page_y_page_size_tal_cual() {
        GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
        when(repository.findHistoryPage(query, 2, 50)).thenReturn(PageResult.empty(2, 50));

        service.execute(query, 2, 50);

        ArgumentCaptor<GetClinicalHistoryQuery> capturado = ArgumentCaptor
                .forClass(GetClinicalHistoryQuery.class);
        verify(repository).findHistoryPage(capturado.capture(), org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(50));
        assertThat(capturado.getValue()).isEqualTo(query);
    }

    @Test
    @DisplayName("una página vacía no es un error")
    void una_pagina_vacia_no_es_un_error() {
        GetClinicalHistoryQuery query = ClinicalHistoryMother.getHistoryQuery();
        when(repository.findHistoryPage(query, 0, 20)).thenReturn(PageResult.empty(0, 20));

        PageResult<ClinicalEventDto> resultado = service.execute(query, 0, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }
}
