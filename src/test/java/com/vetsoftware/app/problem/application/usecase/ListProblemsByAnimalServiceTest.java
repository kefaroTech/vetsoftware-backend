package com.vetsoftware.app.problem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.out.ProblemRepository;
import com.vetsoftware.app.problem.application.query.ListProblemsByAnimalQuery;
import com.vetsoftware.app.problem.domain.Problem;
import com.vetsoftware.app.problem.testsupport.ProblemMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListProblemsByAnimalService")
class ListProblemsByAnimalServiceTest {

    @Mock
    private ProblemRepository repository;

    @InjectMocks
    private ListProblemsByAnimalService service;

    @Test
    @DisplayName("mapea la pagina de dominio a DTO conservando los metadatos de paginacion")
    void mapea_la_pagina_conservando_los_metadatos() {
        Problem problem = ProblemMother.activo();
        PageResult<Problem> pagina = new PageResult<>(List.of(problem), 0, 20, 1L, 1);
        when(repository.findByAnimalIdAndCompanyId(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID, 0, 20)).thenReturn(pagina);

        PageResult<ProblemDto> resultado = service.execute(new ListProblemsByAnimalQuery(
                ProblemMother.ANIMAL_ID, ProblemMother.COMPANY_ID, 0, 20));

        assertThat(resultado.content()).extracting(ProblemDto::id)
                .containsExactly(ProblemMother.PROBLEM_ID);
        assertThat(resultado.page()).isZero();
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(1L);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("un animal sin problemas devuelve una pagina vacia, no null")
    void un_animal_sin_problemas_devuelve_pagina_vacia() {
        when(repository.findByAnimalIdAndCompanyId(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID, 0, 20)).thenReturn(PageResult.empty(0, 20));

        PageResult<ProblemDto> resultado = service.execute(new ListProblemsByAnimalQuery(
                ProblemMother.ANIMAL_ID, ProblemMother.COMPANY_ID, 0, 20));

        assertThat(resultado.content()).isEmpty();
    }

    @Test
    @DisplayName("consulta siempre acotando por animal, empresa, pagina y tamano")
    void consulta_siempre_acotando_por_los_cuatro_parametros() {
        when(repository.findByAnimalIdAndCompanyId(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID, 2, 10)).thenReturn(PageResult.empty(2, 10));

        service.execute(new ListProblemsByAnimalQuery(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID, 2, 10));

        verify(repository).findByAnimalIdAndCompanyId(ProblemMother.ANIMAL_ID,
                ProblemMother.COMPANY_ID, 2, 10);
    }
}
