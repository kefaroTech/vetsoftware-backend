package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListCatalogItemDependenciesService — reglas de un artículo")
class ListCatalogItemDependenciesServiceTest {

    @Mock
    private CatalogItemDependencyRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;
    @InjectMocks
    private ListCatalogItemDependenciesService service;

    @Test
    @DisplayName("proyecta las reglas del artículo")
    void proyecta_las_reglas() {
        when(catalogItemRepository.findById(1L))
                .thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(repository.findAllByCatalogItemId(1L)).thenReturn(
                List.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.REQUIRES),
                        CatalogItemMother.dependencia(10L, 1L, 3L, RelationType.EXCLUDES)));

        List<CatalogItemDependencyDto> dtos = service.listByCatalogItem(1L);

        assertThat(dtos).extracting(CatalogItemDependencyDto::relationType)
                .containsExactly(RelationType.REQUIRES, RelationType.EXCLUDES);
    }

    @Test
    @DisplayName("404 si el artículo no existe")
    void articulo_inexistente() {
        when(catalogItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByCatalogItem(1L))
                .isInstanceOf(CatalogItemNotFoundException.class);

        verifyNoInteractions(repository);
    }
}
