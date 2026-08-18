package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.testsupport.HospitalizationProgressNoteMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListHospitalizationProgressNotesByHospitalizationService")
class ListHospitalizationProgressNotesByHospitalizationServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @InjectMocks
    private ListHospitalizationProgressNotesByHospitalizationService service;

    @Nested
    @DisplayName("Listado")
    class Listado {

        @Test
        @DisplayName("delega en el repositorio y mapea el contenido a dto conservando los metadatos")
        void delega_y_mapea_el_contenido() {
            Long hospitalizationId = HospitalizationProgressNoteMother.HOSPITALIZATION_ID;
            Long companyId = HospitalizationProgressNoteMother.COMPANY_ID;
            HospitalizationProgressNote nota = HospitalizationProgressNoteMother.notaEvolucion();
            when(repository.findAllByHospitalizationIdAndCompanyId(hospitalizationId, companyId, 0,
                    20)).thenReturn(new PageResult<>(List.of(nota), 0, 20, 1L, 1));

            PageResult<HospitalizationProgressNoteDto> pagina = service
                    .listByHospitalization(hospitalizationId, companyId, 0, 20);

            assertThat(pagina.content()).extracting(HospitalizationProgressNoteDto::id)
                    .containsExactly(nota.getId());
            assertThat(pagina.page()).isZero();
            assertThat(pagina.pageSize()).isEqualTo(20);
            assertThat(pagina.totalElements()).isEqualTo(1L);
            assertThat(pagina.totalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("sin resultados devuelve una pagina vacia")
        void sin_resultados_devuelve_pagina_vacia() {
            Long hospitalizationId = HospitalizationProgressNoteMother.HOSPITALIZATION_ID;
            Long companyId = HospitalizationProgressNoteMother.COMPANY_ID;
            when(repository.findAllByHospitalizationIdAndCompanyId(hospitalizationId, companyId, 0,
                    20)).thenReturn(PageResult.empty(0, 20));

            PageResult<HospitalizationProgressNoteDto> pagina = service
                    .listByHospitalization(hospitalizationId, companyId, 0, 20);

            assertThat(pagina.content()).isEmpty();
            assertThat(pagina.totalElements()).isZero();
        }
    }
}
