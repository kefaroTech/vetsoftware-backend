package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("DeleteDiagnosticImagingService")
class DeleteDiagnosticImagingServiceTest {

    private static final Long EMPRESA = DiagnosticImagingMother.COMPANY_ID;
    private static final Long OTRA_EMPRESA = DiagnosticImagingMother.OTRA_EMPRESA.id();

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private DeleteDiagnosticImagingService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("borra la imagen que existe en la empresa del actor")
        void borra_la_imagen_que_existe() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));

            service.execute(DiagnosticImagingMother.IMAGING_ID, EMPRESA);

            verify(repository).delete(DiagnosticImagingMother.IMAGING_ID);
        }

        @Test
        @DisplayName("sin companyId (actor global) busca por id global antes de borrar")
        void sin_company_id_busca_por_id_global() {
            when(repository.findById(DiagnosticImagingMother.IMAGING_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));

            service.execute(DiagnosticImagingMother.IMAGING_ID, null);

            verify(repository).delete(DiagnosticImagingMother.IMAGING_ID);
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("una imagen que no existe lanza DiagnosticImagingNotFoundException y no borra")
        void imagen_inexistente() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(DiagnosticImagingMother.IMAGING_ID, EMPRESA))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verify(repository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("un estudio de otra empresa es un 404 y no se borra nada")
        void estudio_de_otra_empresa_no_se_borra() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID, OTRA_EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(DiagnosticImagingMother.IMAGING_ID, OTRA_EMPRESA))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class);

            verify(repository, never()).delete(any());
            verify(repository, never()).findById(any());
        }
    }
}
