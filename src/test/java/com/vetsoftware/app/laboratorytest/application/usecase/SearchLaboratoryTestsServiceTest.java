package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import com.vetsoftware.app.laboratorytest.testsupport.LaboratoryTestMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchLaboratoryTestsService")
class SearchLaboratoryTestsServiceTest {

    private static final Long COMPANY_ID = LaboratoryTestMother.CLINICA.id();

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private SearchLaboratoryTestsService service;

    private static SearchLaboratoryTestsCommand comando() {
        return new SearchLaboratoryTestsCommand(COMPANY_ID, null, List.of(), null, null, null, null,
                null, 0, 20);
    }

    @Test
    @DisplayName("traduce la pagina de dominio que arma el repositorio a una pagina de dto")
    void traduce_la_pagina_de_dominio_a_dto() {
        LaboratoryTest muestra = LaboratoryTestMother.validada();
        when(repository.search(comando()))
                .thenReturn(new PageResult<>(List.of(muestra), 0, 20, 1L, 1));

        PageResult<LaboratoryTestDto> pagina = service.execute(comando());

        assertThat(pagina.content()).extracting(LaboratoryTestDto::id)
                .containsExactly(LaboratoryTestMother.ID);
    }

    @Test
    @DisplayName("pasa el comando tal cual al repositorio, sin recalcular filtros")
    void pasa_el_comando_tal_cual_al_repositorio() {
        SearchLaboratoryTestsCommand comando = comando();
        when(repository.search(comando)).thenReturn(new PageResult<>(List.of(), 0, 20, 0L, 0));

        service.execute(comando);

        verify(repository).search(comando);
    }
}
