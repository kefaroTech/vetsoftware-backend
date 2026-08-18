package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@DisplayName("ReactivateDiagnosticImagingService")
class ReactivateDiagnosticImagingServiceTest {

    private static final Long EMPRESA = DiagnosticImagingMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = DiagnosticImagingMother.OTRA_EMPRESA.id();

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private ReactivateDiagnosticImagingService service;

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("reactiva y devuelve el DTO recien leido")
        void reactiva_y_devuelve_el_dto() {
            when(repository.reactivate(DiagnosticImagingMother.IMAGING_ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));

            DiagnosticImagingDto dto = service.execute(DiagnosticImagingMother.IMAGING_ID, EMPRESA);

            assertThat(dto.id()).isEqualTo(DiagnosticImagingMother.IMAGING_ID);
            assertThat(dto.enabled()).isTrue();
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("reactivate() en cero lanza not-found sin volver a leer")
        void reactivate_en_cero_lanza_not_found() {
            when(repository.reactivate(DiagnosticImagingMother.IMAGING_ID, EMPRESA)).thenReturn(0);

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }

        @Test
        @DisplayName("reactivate() en uno pero sin fila visible lanza not-found igualmente")
        void reactivate_en_uno_pero_sin_fila_visible() {
            when(repository.reactivate(DiagnosticImagingMother.IMAGING_ID, EMPRESA)).thenReturn(1);
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        /**
         * En reactivate no hay lectura previa: el UPDATE acotado por empresa es la
         * unica barrera y cero filas es el 404, sin revelar que el id existe.
         */
        @Test
        @DisplayName("un estudio de otra empresa no se reactiva y no se relee")
        void estudio_de_otra_empresa_no_se_reactiva() {
            when(repository.reactivate(DiagnosticImagingMother.IMAGING_ID, OTRA_EMPRESA))
                    .thenReturn(0);

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingMother.IMAGING_ID, OTRA_EMPRESA))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
            verify(repository, never()).findById(any());
        }
    }
}
