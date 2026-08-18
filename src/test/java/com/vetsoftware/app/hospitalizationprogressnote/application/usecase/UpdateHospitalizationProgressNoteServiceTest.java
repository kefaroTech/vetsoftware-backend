package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.UpdateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import com.vetsoftware.app.hospitalizationprogressnote.testsupport.HospitalizationProgressNoteMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateHospitalizationProgressNoteService")
class UpdateHospitalizationProgressNoteServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @InjectMocks
    private UpdateHospitalizationProgressNoteService service;

    @Nested
    @DisplayName("Actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza la descripcion de la nota existente y la persiste")
        void actualiza_la_descripcion_y_persiste() {
            UpdateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId())).thenReturn(
                    Optional.of(HospitalizationProgressNoteMother.notaEvolucion(command.id())));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            HospitalizationProgressNoteDto dto = service.execute(command);

            ArgumentCaptor<HospitalizationProgressNote> guardada = ArgumentCaptor
                    .forClass(HospitalizationProgressNote.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getDescription()).isEqualTo(command.description());
            assertThat(dto.description()).isEqualTo(command.description());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza NotFoundException y no guarda si la nota no existe")
        void lanza_not_found_y_no_guarda() {
            UpdateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoActualizar();
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + command.id());

            verify(repository, never()).save(any());
        }
    }

    /**
     * El {@code @PreAuthorize} solo prueba que el atacante declara SU propia
     * empresa; mientras la carga fue {@code findById(command.id())} el gate era
     * vacuo y la nota de evolucion de otra empresa se editaba adivinando el id.
     */
    @Nested
    @DisplayName("aislamiento multi-tenant")
    class Tenencia {

        @Test
        @DisplayName("una nota de otra empresa no se actualiza: 404 y no persiste nada")
        void una_nota_de_otra_empresa_no_se_actualiza() {
            UpdateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoActualizar(HospitalizationProgressNoteMother.OTRA_COMPANY_ID);
            when(repository.findByIdAndCompanyId(command.id(), command.companyId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(HospitalizationProgressNoteNotFoundException.class)
                    .hasMessageContaining("HospitalizationProgressNote not found: " + command.id());

            verify(repository, never()).save(any());
        }
    }
}
