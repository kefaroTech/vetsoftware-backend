package com.vetsoftware.app.problem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.ProblemNotFoundException;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteProblemService")
class DeleteProblemServiceTest {

    @Mock
    private ProblemRepository repository;

    @InjectMocks
    private DeleteProblemService service;

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("borra el problema existente acotando por empresa")
        void borra_el_problema_existente_acotando_por_empresa() {
            when(repository.findByIdAndCompanyId(ProblemMother.PROBLEM_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.of(ProblemMother.activo()));

            service.execute(ProblemMother.PROBLEM_ID, ProblemMother.COMPANY_ID);

            verify(repository).delete(ProblemMother.PROBLEM_ID, ProblemMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa")
    class AislamientoPorEmpresa {

        @Test
        @DisplayName("un problema de otra empresa no existe y no se borra")
        void un_problema_de_otra_empresa_no_existe() {
            when(repository.findByIdAndCompanyId(ProblemMother.PROBLEM_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(ProblemMother.PROBLEM_ID, ProblemMother.COMPANY_ID))
                    .isInstanceOf(ProblemNotFoundException.class)
                    .hasMessageContaining("Problem not found: " + ProblemMother.PROBLEM_ID);

            verify(repository, never()).delete(ProblemMother.PROBLEM_ID, ProblemMother.COMPANY_ID);
        }
    }
}
