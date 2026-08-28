package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.LimitDimensionQueryPort;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateCatalogItemService — edición de un artículo")
class UpdateCatalogItemServiceTest {

    @Mock
    private CatalogItemRepository repository;
    @Mock
    private LimitDimensionQueryPort limitDimensionQueryPort;
    @InjectMocks
    private UpdateCatalogItemService service;

    private static UpdateCatalogItemCommand comando() {
        return new UpdateCatalogItemCommand(1L, "Historia clínica PRO", "Corta", "Larga",
                ItemType.MODULE, null, false, 2, 9, 3, CatalogItemStatus.DEPRECATED);
    }

    @Test
    @DisplayName("actualiza lo comercial y no toca el código, que es inmutable")
    void actualiza_sin_tocar_el_codigo() {
        when(repository.findById(1L)).thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        CatalogItemDto dto = service.execute(comando());

        ArgumentCaptor<CatalogItem> guardado = ArgumentCaptor.forClass(CatalogItem.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCode()).isEqualTo("CLINICAL_HISTORY");
        assertThat(guardado.getValue().getName()).isEqualTo("Historia clínica PRO");
        assertThat(dto.status()).isEqualTo(CatalogItemStatus.DEPRECATED);
    }

    @Test
    @DisplayName("404 si el artículo no existe, y no escribe nada")
    void articulo_inexistente() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 1");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante del dominio sin llegar a guardar")
    void invariante_de_dominio() {
        when(repository.findById(1L)).thenReturn(Optional.of(CatalogItemMother.historiaClinica()));

        assertThatThrownBy(() -> service.execute(new UpdateCatalogItemCommand(1L, "Nombre", null,
                null, ItemType.CAPACITY, null, false, 1, null, 0, CatalogItemStatus.ACTIVE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacityUnit is required for CAPACITY");

        verify(repository, never()).save(any());
    }
}
