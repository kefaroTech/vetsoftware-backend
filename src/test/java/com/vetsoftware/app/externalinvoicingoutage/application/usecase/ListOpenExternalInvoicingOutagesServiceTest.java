package com.vetsoftware.app.externalinvoicingoutage.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicingoutage.application.dto.ExternalInvoicingOutageDto;
import com.vetsoftware.app.externalinvoicingoutage.application.port.out.ExternalInvoicingOutageRepository;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListOpenExternalInvoicingOutagesService")
class ListOpenExternalInvoicingOutagesServiceTest {

    @Mock
    private ExternalInvoicingOutageRepository repository;

    @InjectMocks
    private ListOpenExternalInvoicingOutagesService service;

    @Test
    @DisplayName("mapea las caidas vivas sin paginar")
    void mapea_las_caidas_vivas_sin_paginar() {
        ExternalInvoicingOutage viva = ExternalInvoicingOutageMother.abierta();
        when(repository.findAllOpen()).thenReturn(List.of(viva));

        List<ExternalInvoicingOutageDto> abiertas = service.listOpen();

        assertThat(abiertas).hasSize(1);
        assertThat(abiertas.get(0).id()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
        assertThat(abiertas.get(0).open()).isTrue();
    }

    @Test
    @DisplayName("sin ninguna caida viva devuelve una lista vacia, no un error")
    void sin_ninguna_caida_viva_devuelve_una_lista_vacia() {
        when(repository.findAllOpen()).thenReturn(List.of());

        assertThat(service.listOpen()).isEmpty();
    }
}
