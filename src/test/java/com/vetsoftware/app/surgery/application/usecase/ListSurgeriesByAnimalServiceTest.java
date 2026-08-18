package com.vetsoftware.app.surgery.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.Surgery;
import com.vetsoftware.app.surgery.testsupport.SurgeryMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListSurgeriesByAnimalService")
class ListSurgeriesByAnimalServiceTest {

    @Mock
    private SurgeryRepository repository;

    @InjectMocks
    private ListSurgeriesByAnimalService service;

    @Test
    @DisplayName("mapea el contenido a DTO conservando la paginacion del repositorio")
    void mapea_el_contenido_conservando_la_paginacion() {
        Surgery uno = SurgeryMother.cirugiaValida(1L);
        Surgery dos = SurgeryMother.cirugiaValida(2L);
        PageResult<Surgery> pagina = new PageResult<>(List.of(uno, dos), 0, 20, 2L, 1);
        when(repository.findAllByAnimalIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                SurgeryMother.COMPANY_ID, "sangrado", 0, 20)).thenReturn(pagina);

        PageResult<SurgeryDto> resultado = service.listByAnimal(SurgeryMother.ANIMAL_ID,
                SurgeryMother.COMPANY_ID, "sangrado", 0, 20);

        assertThat(resultado.content()).extracting(SurgeryDto::id).containsExactly(1L, 2L);
        assertThat(resultado.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAllByAnimalIdAndCompanyId(SurgeryMother.ANIMAL_ID,
                SurgeryMother.COMPANY_ID, null, 3, 20))
                .thenReturn(new PageResult<>(List.of(), 3, 20, 40L, 2));

        PageResult<SurgeryDto> resultado = service.listByAnimal(SurgeryMother.ANIMAL_ID,
                SurgeryMother.COMPANY_ID, null, 3, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(40L);
    }
}
