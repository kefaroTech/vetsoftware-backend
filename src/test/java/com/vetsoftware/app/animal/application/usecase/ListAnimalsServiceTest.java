package com.vetsoftware.app.animal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.dto.PageResult;
import com.vetsoftware.app.animal.application.port.out.AnimalRepository;
import com.vetsoftware.app.animal.domain.Animal;
import com.vetsoftware.app.animal.testsupport.AnimalMother;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListAnimalsService")
class ListAnimalsServiceTest {

    @Mock
    private AnimalRepository repository;

    @InjectMocks
    private ListAnimalsService service;

    @Test
    @DisplayName("mapea el contenido a DTO conservando la paginacion del repositorio")
    void mapea_el_contenido_conservando_la_paginacion() {
        PageResult<Animal> pagina = new PageResult<>(
                List.of(AnimalMother.perroSano(1L), AnimalMother.perroSano(2L)), 2, 20, 57L, 3);
        when(repository.findAllByCompanyId(AnimalMother.COMPANY_ID, 2, 20)).thenReturn(pagina);

        PageResult<AnimalDto> resultado = service.listAll(AnimalMother.COMPANY_ID, 2, 20);

        assertThat(resultado.content()).extracting(AnimalDto::id).containsExactly(1L, 2L);
        assertThat(resultado.page()).isEqualTo(2);
        assertThat(resultado.pageSize()).isEqualTo(20);
        assertThat(resultado.totalElements()).isEqualTo(57L);
        assertThat(resultado.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("respeta el orden que devuelve el repositorio")
    void respeta_el_orden_que_devuelve_el_repositorio() {
        PageResult<Animal> pagina = new PageResult<>(List.of(AnimalMother.perroSano(3L),
                AnimalMother.perroSano(1L), AnimalMother.perroSano(2L)), 0, 20, 3L, 1);
        when(repository.findAllByCompanyId(AnimalMother.COMPANY_ID, 0, 20)).thenReturn(pagina);

        PageResult<AnimalDto> resultado = service.listAll(AnimalMother.COMPANY_ID, 0, 20);

        assertThat(resultado.content()).extracting(AnimalDto::id).containsExactly(3L, 1L, 2L);
    }

    @Test
    @DisplayName("una pagina vacia no es un error")
    void una_pagina_vacia_no_es_un_error() {
        when(repository.findAllByCompanyId(AnimalMother.COMPANY_ID, 9, 20))
                .thenReturn(new PageResult<>(List.of(), 9, 20, 57L, 3));

        PageResult<AnimalDto> resultado = service.listAll(AnimalMother.COMPANY_ID, 9, 20);

        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(57L);
    }

    @Test
    @DisplayName("pasa page y pageSize tal cual: el service NO los valida ni los acota")
    void pasa_page_y_page_size_tal_cual() {
        // Comportamiento actual, fijado a proposito: cualquier saneamiento vive en el
        // controller o en el adaptador. Si un dia se decide acotar pageSize para que
        // un cliente no pida 10.000 filas, este test es el que tiene que cambiar.
        when(repository.findAllByCompanyId(AnimalMother.COMPANY_ID, -1, 10_000))
                .thenReturn(new PageResult<>(List.of(), -1, 10_000, 0L, 0));

        service.listAll(AnimalMother.COMPANY_ID, -1, 10_000);

        verify(repository).findAllByCompanyId(AnimalMother.COMPANY_ID, -1, 10_000);
    }
}
