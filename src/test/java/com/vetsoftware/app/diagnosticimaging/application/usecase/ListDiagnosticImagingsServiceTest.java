package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDiagnosticImagingsService")
class ListDiagnosticImagingsServiceTest {

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private ListDiagnosticImagingsService service;

    @Test
    @DisplayName("mapea todas las imagenes diagnosticas a DTOs")
    void mapea_todas_las_imagenes_a_dtos() {
        when(repository.findAll()).thenReturn(List.of(DiagnosticImagingMother.persistida()));

        List<DiagnosticImagingDto> dtos = service.listAll();

        assertThat(dtos).extracting(DiagnosticImagingDto::id)
                .containsExactly(DiagnosticImagingMother.IMAGING_ID);
    }
}
