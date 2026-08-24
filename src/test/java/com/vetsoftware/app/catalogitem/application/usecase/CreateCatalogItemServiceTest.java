package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CapacityUnit;
import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import com.vetsoftware.app.catalogitem.domain.CatalogItemCodeAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemStatus;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCatalogItemService — alta de un artículo del catálogo")
class CreateCatalogItemServiceTest {

    @Mock
    private CatalogItemRepository repository;

    private CreateCatalogItemService service;

    @BeforeEach
    void setUp() {
        service = new CreateCatalogItemService(repository, CatalogItemMother.RELOJ);
    }

    @Test
    @DisplayName("persiste el artículo con la fecha del reloj inyectado y habilitado")
    void persiste_el_articulo() {
        when(repository.existsByCodeIgnoringEnabled("CLINICAL_HISTORY")).thenReturn(false);
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        CatalogItemDto dto = service.execute(CatalogItemMother.comandoValido());

        ArgumentCaptor<CatalogItem> guardado = ArgumentCaptor.forClass(CatalogItem.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getCode()).isEqualTo("CLINICAL_HISTORY");
        assertThat(guardado.getValue().getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        assertThat(guardado.getValue().isEnabled()).isTrue();
        assertThat(dto.code()).isEqualTo("CLINICAL_HISTORY");
        assertThat(dto.status()).isEqualTo(CatalogItemStatus.ACTIVE);
    }

    /**
     * La comprobación ignora el borrado lógico porque {@code uq_catalog_items_code}
     * también lo ignora: dejar que salte la constraint convertiría un duplicado en
     * un 500 sin decir qué código chocó.
     */
    @Test
    @DisplayName("409 si el código ya existe, incluso desactivado, y no escribe nada")
    void codigo_duplicado() {
        when(repository.existsByCodeIgnoringEnabled("CLINICAL_HISTORY")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(CatalogItemMother.comandoValido()))
                .isInstanceOf(CatalogItemCodeAlreadyExistsException.class)
                .hasMessageContaining("CLINICAL_HISTORY");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante del dominio sin llegar a guardar")
    void invariante_de_dominio() {
        when(repository.existsByCodeIgnoringEnabled("BAD")).thenReturn(false);

        CreateCatalogItemCommand command = new CreateCatalogItemCommand("BAD", "Malo", null, null,
                ItemType.MODULE, CapacityUnit.USER, false, 1, null, 0, CatalogItemStatus.DRAFT);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only allowed on CAPACITY items");

        verify(repository, never()).save(any());
    }
}
