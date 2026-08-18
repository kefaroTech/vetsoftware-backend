package com.vetsoftware.app.problem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.problem.application.command.CreateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.problem.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.domain.ProblemStatus;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
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
@DisplayName("CreateProblemService")
class CreateProblemServiceTest {

    @Mock
    private ProblemRepository repository;
    @Mock
    private AnimalQueryPort animalQueryPort;
    @Mock
    private CompanyQueryPort companyQueryPort;

    @InjectMocks
    private CreateProblemService service;

    @Captor
    private ArgumentCaptor<Problem> problemCaptor;

    private void referenciasExisten() {
        when(animalQueryPort.findByIdAndCompanyId(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID)).thenReturn(Optional.of(ProblemMother.FIRULAIS));
        when(companyQueryPort.findById(ProblemMother.COMPANY_ID))
                .thenReturn(Optional.of(ProblemMother.CLINICA));
    }

    @Nested
    @DisplayName("creacion")
    class Creacion {

        @Test
        @DisplayName("persiste el problema con las referencias resueltas por los puertos")
        void persiste_el_problema_con_las_referencias_resueltas() {
            referenciasExisten();
            when(repository.save(any())).thenReturn(ProblemMother.activo());

            service.execute(ProblemMother.comandoCrear());

            verify(repository).save(problemCaptor.capture());
            Problem guardado = problemCaptor.getValue();
            // Lo que importa no es que se llamara a save, sino que se guardara ESTO: las
            // refs tienen que venir de los puertos, no de los ids del comando.
            assertThat(guardado.getAnimal()).isEqualTo(ProblemMother.FIRULAIS);
            assertThat(guardado.getCompany()).isEqualTo(ProblemMother.CLINICA);
            assertThat(guardado.getDescription()).isEqualTo("Dermatitis alergica");
            assertThat(guardado.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
            assertThat(guardado.getId()).isNull();
            assertThat(guardado.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO del problema ya persistido, con su id")
        void devuelve_el_dto_del_problema_ya_persistido() {
            referenciasExisten();
            when(repository.save(any())).thenReturn(ProblemMother.activo());

            ProblemDto dto = service.execute(ProblemMother.comandoCrear());

            assertThat(dto.id()).isEqualTo(ProblemMother.PROBLEM_ID);
            assertThat(dto.animalName()).isEqualTo("Firulais");
        }
    }

    @Nested
    @DisplayName("referencias que no existen")
    class ReferenciasInexistentes {

        @Test
        @DisplayName("animal inexistente: no consulta la empresa ni persiste")
        void animal_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(ProblemMother.ANIMAL_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProblemMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Animal not found: " + ProblemMother.ANIMAL_ID);

            verifyNoInteractions(companyQueryPort, repository);
        }

        @Test
        @DisplayName("empresa inexistente: no persiste")
        void empresa_inexistente() {
            when(animalQueryPort.findByIdAndCompanyId(ProblemMother.ANIMAL_ID,
                    ProblemMother.COMPANY_ID)).thenReturn(Optional.of(ProblemMother.FIRULAIS));
            when(companyQueryPort.findById(ProblemMother.COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(ProblemMother.comandoCrear()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Company not found: " + ProblemMother.COMPANY_ID);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("invariantes del dominio propagadas")
    class InvariantesDelDominio {

        @Test
        @DisplayName("una descripcion en blanco no llega a persistirse")
        void una_descripcion_en_blanco_no_llega_a_persistirse() {
            referenciasExisten();
            CreateProblemCommand comando = new CreateProblemCommand(ProblemMother.ANIMAL_ID, "  ",
                    ProblemStatus.ACTIVE, ProblemMother.INICIO, null, null,
                    ProblemMother.COMPANY_ID);

            assertThatThrownBy(() -> service.execute(comando))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");

            verify(repository, never()).save(any());
        }
    }
}
