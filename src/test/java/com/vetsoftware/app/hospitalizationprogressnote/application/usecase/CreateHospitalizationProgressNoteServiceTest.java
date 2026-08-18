package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationQueryPort;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
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
@DisplayName("CreateHospitalizationProgressNoteService")
class CreateHospitalizationProgressNoteServiceTest {

    @Mock
    private HospitalizationProgressNoteRepository repository;
    @Mock
    private HospitalizationQueryPort hospitalizationQueryPort;
    @Mock
    private EmployeeQueryPort employeeQueryPort;
    @InjectMocks
    private CreateHospitalizationProgressNoteService service;

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("crea la nota con la hospitalizacion y el empleado resueltos por los puertos")
        void crea_la_nota_con_las_referencias_resueltas() {
            CreateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoCrear();
            when(hospitalizationQueryPort.findById(command.hospitalizationId()))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.HOSPITALIZACION));
            when(employeeQueryPort.findById(command.createdById()))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.VETERINARIO));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            HospitalizationProgressNoteDto dto = service.execute(command);

            ArgumentCaptor<HospitalizationProgressNote> guardada = ArgumentCaptor
                    .forClass(HospitalizationProgressNote.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getDescription()).isEqualTo(command.description());
            assertThat(guardada.getValue().getHospitalization())
                    .isEqualTo(HospitalizationProgressNoteMother.HOSPITALIZACION);
            assertThat(guardada.getValue().getCreatedBy())
                    .isEqualTo(HospitalizationProgressNoteMother.VETERINARIO);
            assertThat(dto.description()).isEqualTo(command.description());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el repositorio ni al empleado si la hospitalizacion no existe")
        void no_toca_nada_si_la_hospitalizacion_no_existe() {
            CreateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoCrear();
            when(hospitalizationQueryPort.findById(command.hospitalizationId()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "Hospitalization not found: " + command.hospitalizationId());

            verifyNoInteractions(employeeQueryPort, repository);
        }

        @Test
        @DisplayName("no toca el repositorio si el empleado creador no existe")
        void no_toca_el_repositorio_si_el_empleado_no_existe() {
            CreateHospitalizationProgressNoteCommand command = HospitalizationProgressNoteMother
                    .comandoCrear();
            when(hospitalizationQueryPort.findById(command.hospitalizationId()))
                    .thenReturn(Optional.of(HospitalizationProgressNoteMother.HOSPITALIZACION));
            when(employeeQueryPort.findById(command.createdById())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Employee not found: " + command.createdById());

            verifyNoInteractions(repository);
        }
    }
}
