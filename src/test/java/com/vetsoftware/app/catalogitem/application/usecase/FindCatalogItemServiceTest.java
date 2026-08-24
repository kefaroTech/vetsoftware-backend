package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindCatalogItemService — consulta por id")
class FindCatalogItemServiceTest {

    @Mock
    private CatalogItemRepository repository;
    @InjectMocks
    private FindCatalogItemService service;

    @Test
    @DisplayName("proyecta el artículo campo por campo")
    void proyecta_el_articulo() {
        when(repository.findById(1L)).thenReturn(Optional.of(CatalogItemMother.historiaClinica()));

        CatalogItemDto dto = service.findById(1L);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.code()).isEqualTo("CLINICAL_HISTORY");
        assertThat(dto.itemType()).isEqualTo(ItemType.MODULE);
        assertThat(dto.core()).isTrue();
        assertThat(dto.capacityUnit()).isNull();
    }

    @Test
    @DisplayName("404 si el artículo no existe")
    void articulo_inexistente() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 1");
    }
}
