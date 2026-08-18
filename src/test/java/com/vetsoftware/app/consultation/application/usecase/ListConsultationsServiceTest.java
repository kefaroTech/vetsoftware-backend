package com.vetsoftware.app.consultation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.port.out.ConsultationRepository;
import com.vetsoftware.app.consultation.domain.Consultation;
import com.vetsoftware.app.consultation.testsupport.ConsultationMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListConsultationsService")
class ListConsultationsServiceTest {

    @Mock
    private ConsultationRepository repository;

    @InjectMocks
    private ListConsultationsService service;

    @Test
    @DisplayName("mapea el contenido a DTO conservando la paginacion del repositorio")
    void mapea_el_contenido_conservando_la_paginacion() {
        Consultation uno = ConsultationMother.consultaValida(1L);
        Consultation dos = ConsultationMother.consultaValida(2L);
        PageResult<Consultation> pagina = new PageResult<>(List.of(uno, dos), 2, 20, 57L, 3);
        when(repository.findAllByCompanyId(ConsultationMother.COMPANY_ID, 2, 20))
                .thenReturn(pagina);

        PageResult<ConsultationDto> resultado = service.listAll(ConsultationMother.COMPANY_ID, 2,
                20);

        assertThat(resultado.content()).extracting(ConsultationDto::id).containsExactly(1L, 2L);
        assertThat(resultado.page()).isEqualTo(2);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(57L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAllByCompanyId(ConsultationMother.COMPANY_ID, 9, 20))
                .thenReturn(new PageResult<>(List.of(), 9, 20, 57L, 3));

        PageResult<ConsultationDto> resultado = service.listAll(ConsultationMother.COMPANY_ID, 9,
                20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(57L);
    }
}
