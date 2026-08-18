package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDiagnosticImagingsByAnimalService")
class ListDiagnosticImagingsByAnimalServiceTest {

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private ListDiagnosticImagingsByAnimalService service;

    @Test
    @DisplayName("mapea la pagina de dominio a una pagina de DTOs")
    void mapea_la_pagina_de_dominio_a_dtos() {
        DiagnosticImaging imaging = DiagnosticImagingMother.persistida();
        PageResult<DiagnosticImaging> pagina = PageResult.of(List.of(imaging), 0, 20, 1L);
        when(repository.findAllByAnimalIdAndCompanyId(DiagnosticImagingMother.ANIMAL_ID,
                DiagnosticImagingMother.COMPANY_ID, "cadera", 0, 20)).thenReturn(pagina);

        PageResult<DiagnosticImagingDto> resultado = service.listByAnimal(
                DiagnosticImagingMother.ANIMAL_ID, DiagnosticImagingMother.COMPANY_ID, "cadera", 0,
                20);

        assertThat(resultado.content()).extracting(DiagnosticImagingDto::id)
                .containsExactly(DiagnosticImagingMother.IMAGING_ID);
        assertThat(resultado.totalElements()).isEqualTo(1L);
    }
}
