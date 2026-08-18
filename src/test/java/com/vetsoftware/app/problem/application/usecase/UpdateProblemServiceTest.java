package com.vetsoftware.app.problem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.time.LocalDate;
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
@DisplayName("UpdateProblemService")
class UpdateProblemServiceTest {

    @Mock
    private ProblemRepository repository;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private UpdateProblemService service;

    @Captor
    private ArgumentCaptor<Problem> problemCaptor;

    private void problemaYEmpresaExisten() {
        when(repository.findByIdAndCompanyId(ProblemMother.PROBLEM_ID, ProblemMother.COMPANY_ID))
                .thenReturn(Optional.of(ProblemMother.activo()));
        when(companyQueryPort.findById(ProblemMother.COMPANY_ID))
                .thenReturn(Optional.of(ProblemMother.CLINICA));
    }

    @Nested
    @DisplayName("actualizacion")
    class Actualizacion {

        @Test
        @DisplayName("actualiza el problema encontrado y lo persiste con la empresa resuelta")
        void actualiza_el_problema_encontrado() {
            problemaYEmpresaExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(ProblemMother.comandoActualizar());

            verify(repository).save(problemCaptor.capture());
            Problem guardado = problemCaptor.getValue();
            assertThat(guardado.getDescription()).isEqualTo("Resuelto tras tratamiento");
            assertThat(guardado.getStatus()).isEqualTo(ProblemStatus.RESOLVED);
            assertThat(guardado.getResolvedDate()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(guardado.getCompany()).isEqualTo(ProblemMother.CLINICA);
        }

        @Test
        @DisplayName("devuelve el DTO del problema actualizado")
        void devuelve_el_dto_del_problema_actualizado() {
            problemaYEmpresaExisten();
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProblemDto dto = service.execute(ProblemMother.comandoActualizar());

            assertThat(dto.status()).isEqualTo(ProblemStatus.RESOLVED);
        }
    }

    @Nested
    @DisplayName("problema o empresa inexistentes")
    class Inexistentes {

        @Test
        @DisplayName("problema de otra empresa: no consulta la empresa ni persiste")
        void problema_inexistente() {
            when(repository.findByIdAndCompanyId(ProblemMother.PROBLEM_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProblemMother.comandoActualizar()))
                    .isInstanceOf(ProblemNotFoundException.class)
                    .hasMessageContaining("Problem not found: " + ProblemMother.PROBLEM_ID);

            verifyNoInteractions(companyQueryPort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente() {
            when(repository.findByIdAndCompanyId(ProblemMother.PROBLEM_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.of(ProblemMother.activo()));
            when(companyQueryPort.findById(ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProblemMother.comandoActualizar()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ProblemMother.COMPANY_ID);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una descripcion en blanco no llega a persistirse")
        void una_descripcion_en_blanco_no_llega_a_persistirse() {
            problemaYEmpresaExisten();
            UpdateProblemCommand comando = new UpdateProblemCommand(ProblemMother.PROBLEM_ID, "  ",
                    ProblemStatus.RESOLVED, ProblemMother.INICIO, null, null,
                    ProblemMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");

            verify(repository, never()).save(any());
        }
    }
}
