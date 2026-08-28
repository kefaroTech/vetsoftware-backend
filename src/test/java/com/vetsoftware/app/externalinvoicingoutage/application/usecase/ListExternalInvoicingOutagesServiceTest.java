package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListExternalInvoicingOutagesService")
class ListExternalInvoicingOutagesServiceTest {

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @InjectMocks
    private ListExternalInvoicingOutagesService service;

    @Test
    @DisplayName("mapea el historico conservando la paginacion del repositorio")
    void mapea_el_historico_conservando_la_paginacion() {
        ExternalInvoicingOutage primera = ExternalInvoicingOutageMother
                .abierta(ExternalInvoicingOutageMother.OUTAGE_ID);
        ExternalInvoicingOutage segunda = ExternalInvoicingOutageMother
                .abierta(ExternalInvoicingOutageMother.OUTAGE_ID + 1);
        when(repository.findAll(0, 20))
                .thenReturn(new PageResult<>(List.of(primera, segunda), 0, 20, 12L, 1));

        PageResult<ExternalInvoicingOutageDto> resultado = service.listAll(0, 20);

        assertThat(resultado.content()).extracting(ExternalInvoicingOutageDto::id).containsExactly(
                ExternalInvoicingOutageMother.OUTAGE_ID,
                ExternalInvoicingOutageMother.OUTAGE_ID + 1);
        assertThat(resultado.totalElements()).isEqualTo(12L);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAll(5, 20)).thenReturn(new PageResult<>(List.of(), 5, 20, 12L, 1));

        PageResult<ExternalInvoicingOutageDto> resultado = service.listAll(5, 20);

        assertThat(resultado.content()).isEmpty();
    }
}
