package com.vetsoftware.app.laboratorytest.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
@DisplayName("ListLaboratoryTestsByAnimalService")
class ListLaboratoryTestsByAnimalServiceTest {

    private static final Long ANIMAL_ID = LaboratoryTestMother.FIRULAIS.id();
    private static final Long COMPANY_ID = LaboratoryTestMother.CLINICA.id();

    @Mock
    private LaboratoryTestRepository repository;

    @InjectMocks
    private ListLaboratoryTestsByAnimalService service;

    @Test
    @DisplayName("traduce la pagina de dominio a una pagina de dto, conservando los totales")
    void traduce_la_pagina_de_dominio_a_dto() {
        LaboratoryTest muestra = LaboratoryTestMother.pendienteDeToma();
        when(repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, COMPANY_ID, "anemia", 0, 20))
                .thenReturn(new PageResult<>(List.of(muestra), 0, 20, 1L, 1));

        PageResult<LaboratoryTestDto> pagina = service.listByAnimal(ANIMAL_ID, COMPANY_ID, "anemia",
                0, 20);

        assertThat(pagina.content()).extracting(LaboratoryTestDto::id)
                .containsExactly(LaboratoryTestMother.ID);
        assertThat(pagina.totalElements()).isEqualTo(1L);
        assertThat(pagina.totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("pasa el animal, la empresa, el texto y la paginacion tal cual al repositorio")
    void pasa_los_parametros_tal_cual_al_repositorio() {
        when(repository.findAllByAnimalIdAndCompanyId(ANIMAL_ID, COMPANY_ID, null, 1, 10))
                .thenReturn(new PageResult<>(List.of(), 1, 10, 0L, 0));

        service.listByAnimal(ANIMAL_ID, COMPANY_ID, null, 1, 10);

        verify(repository).findAllByAnimalIdAndCompanyId(ANIMAL_ID, COMPANY_ID, null, 1, 10);
    }
}
