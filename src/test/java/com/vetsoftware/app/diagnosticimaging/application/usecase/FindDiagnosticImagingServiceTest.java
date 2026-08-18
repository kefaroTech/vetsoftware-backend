package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindDiagnosticImagingService")
class FindDiagnosticImagingServiceTest {

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private FindDiagnosticImagingService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("devuelve el DTO de la imagen encontrada en la empresa")
        void devuelve_el_dto_de_la_imagen_encontrada() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));

            DiagnosticImagingDto dto = service.findById(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID);

            assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("una imagen inexistente en la empresa lanza DiagnosticImagingNotFoundException")
        void imagen_inexistente() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));
        }
    }
}
