package com.vetsoftware.app.diagnosticimaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.diagnosticimaging.application.command.ChangeDiagnosticImagingStatusCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingStatus;
import com.vetsoftware.app.diagnosticimaging.testsupport.DiagnosticImagingMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeDiagnosticImagingStatusService")
class ChangeDiagnosticImagingStatusServiceTest {

    @Mock
    private DiagnosticImagingRepository repository;

    @InjectMocks
    private ChangeDiagnosticImagingStatusService service;

    @Captor
    private ArgumentCaptor<DiagnosticImaging> captor;

    @Nested
    @DisplayName("companyId presente en el comando")
    class ConCompanyId {

        @Test
        @DisplayName("busca por id y empresa, y normaliza el estado a mayusculas")
        void busca_por_id_y_empresa_y_normaliza_el_estado() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));
            when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            DiagnosticImagingDto dto = service
                    .execute(DiagnosticImagingMother.comandoCambiarEstado("completado"));

            assertThat(captor.getValue().getStatus()).isEqualTo(DiagnosticImagingStatus.COMPLETADO);
            assertThat(dto.status()).isEqualTo("COMPLETADO");
            verify(repository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("companyId ausente en el comando")
    class SinCompanyId {

        @Test
        @DisplayName("busca por id a secas cuando el comando no trae empresa")
        void busca_por_id_a_secas() {
            ChangeDiagnosticImagingStatusCommand comando = new ChangeDiagnosticImagingStatusCommand(
                    DiagnosticImagingMother.IMAGING_ID, "CANCELADO", null);
            when(repository.findById(DiagnosticImagingMother.IMAGING_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));
            when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando);

            assertThat(captor.getValue().getStatus()).isEqualTo(DiagnosticImagingStatus.CANCELADO);
            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("fallos")
    class Fallos {

        @Test
        @DisplayName("una imagen inexistente lanza DiagnosticImagingNotFoundException")
        void imagen_inexistente() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(DiagnosticImagingMother.comandoCambiarEstado("COMPLETADO")))
                    .isInstanceOf(DiagnosticImagingNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DiagnosticImagingMother.IMAGING_ID));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un status que no existe en el enum falla antes de guardar")
        void status_desconocido_falla() {
            when(repository.findByIdAndCompanyId(DiagnosticImagingMother.IMAGING_ID,
                    DiagnosticImagingMother.COMPANY_ID))
                    .thenReturn(Optional.of(DiagnosticImagingMother.persistida()));

            assertThatThrownBy(() -> service
                    .execute(DiagnosticImagingMother.comandoCambiarEstado("EN_PROCESO")))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }
    }
}
