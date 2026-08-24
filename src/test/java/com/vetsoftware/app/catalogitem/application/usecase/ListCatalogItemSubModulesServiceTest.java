package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
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
@DisplayName("ListCatalogItemSubModulesService — submódulos que abre un artículo")
class ListCatalogItemSubModulesServiceTest {

    @Mock
    private CatalogItemSubModuleRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;
    @InjectMocks
    private ListCatalogItemSubModulesService service;

    @Test
    @DisplayName("proyecta los vínculos con su submódulo")
    void proyecta_los_vinculos() {
        when(catalogItemRepository.findById(1L))
                .thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(repository.findAllByCatalogItemId(1L))
                .thenReturn(List.of(CatalogItemMother.vinculo(5L, 1L)));

        List<CatalogItemSubModuleDto> dtos = service.listByCatalogItem(1L);

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).subModule().code()).isEqualTo("CONSULTATIONS");
    }

    /**
     * Devolver una lista vacía para un id inexistente le diría al cliente «este
     * artículo no abre nada» cuando la verdad es «este artículo no existe».
     */
    @Test
    @DisplayName("404 si el artículo no existe, en vez de una lista vacía que miente")
    void articulo_inexistente() {
        when(catalogItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByCatalogItem(1L))
                .isInstanceOf(CatalogItemNotFoundException.class);

        verifyNoInteractions(repository);
    }
}
