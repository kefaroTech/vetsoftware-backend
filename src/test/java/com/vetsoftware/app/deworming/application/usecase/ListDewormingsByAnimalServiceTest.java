package com.vetsoftware.app.deworming.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import com.vetsoftware.app.deworming.domain.Deworming;
import com.vetsoftware.app.deworming.testsupport.DewormingMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListDewormingsByAnimalService")
class ListDewormingsByAnimalServiceTest {

    @Mock
    private DewormingRepository repository;
    @InjectMocks
    private ListDewormingsByAnimalService service;

    @Test
    @DisplayName("traduce la pagina de dominio a DTO conservando los metadatos")
    void traduce_la_pagina_de_dominio_a_dto_conservando_los_metadatos() {
        PageResult<Deworming> pagina = PageResult
                .of(List.of(DewormingMother.desparasitacionValida()), 0, 20, 1L);
        when(repository.findAllByAnimalIdAndCompanyId(DewormingMother.ANIMAL_ID,
                DewormingMother.COMPANY_ID, "drontal", 0, 20)).thenReturn(pagina);

        PageResult<DewormingDto> resultado = service.listByAnimal(DewormingMother.ANIMAL_ID,
                DewormingMother.COMPANY_ID, "drontal", 0, 20);

        assertThat(resultado.content()).hasSize(1);
        assertThat(resultado.content().getFirst().id()).isEqualTo(DewormingMother.DEWORMING_ID);
        assertThat(resultado.totalElements()).isEqualTo(1L);
        assertThat(resultado.page()).isEqualTo(0);
        assertThat(resultado.pageSize()).isEqualTo(20);
    }
}
