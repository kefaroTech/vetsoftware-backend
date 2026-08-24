package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCatalogItemsService — listado paginado del catálogo")
class ListCatalogItemsServiceTest {

    @Mock
    private CatalogItemRepository repository;
    @InjectMocks
    private ListCatalogItemsService service;

    /**
     * Los totales son los de la consulta, no los del contenido de la página.
     * Recalcularlos sobre lo ya paginado es como se acaba reportando «2 de 2» en un
     * catálogo de cuarenta artículos.
     */
    @Test
    @DisplayName("conserva intactos los metadatos de la página al proyectar a DTO")
    void conserva_los_metadatos_de_la_pagina() {
        PageResult<CatalogItem> pagina = new PageResult<>(
                List.of(CatalogItemMother.historiaClinica(), CatalogItemMother.usuarioExtra()), 1,
                2, 40L, 20);
        when(repository.findAll(1, 2)).thenReturn(pagina);

        PageResult<CatalogItemDto> resultado = service.listAll(1, 2);

        assertThat(resultado.page()).isEqualTo(1);
        assertThat(resultado.pageSize()).isEqualTo(2);
        assertThat(resultado.totalElements()).isEqualTo(40L);
        assertThat(resultado.totalPages()).isEqualTo(20);
        assertThat(resultado.content()).extracting(CatalogItemDto::code)
                .containsExactly("CLINICAL_HISTORY", "EXTRA_USER");
    }

    @Test
    @DisplayName("una página vacía sigue siendo una página, no un error")
    void pagina_vacia() {
        when(repository.findAll(0, 20)).thenReturn(PageResult.empty(0, 20));

        assertThat(service.listAll(0, 20).content()).isEmpty();
    }
}
