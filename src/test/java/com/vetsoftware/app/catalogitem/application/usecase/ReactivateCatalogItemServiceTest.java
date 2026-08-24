package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateCatalogItemService — devolver un artículo al catálogo")
class ReactivateCatalogItemServiceTest {

    @Mock
    private CatalogItemRepository repository;
    @InjectMocks
    private ReactivateCatalogItemService service;

    /**
     * Decide por filas actualizadas y no por una lectura previa: el
     * {@code @SQLRestriction} de la entidad esconde justo la fila que se quiere
     * reactivar, así que un {@code findById} devolvería vacío para algo que sí
     * está.
     */
    @Test
    @DisplayName("reactiva y devuelve el artículo ya visible")
    void reactiva_y_devuelve_el_articulo() {
        when(repository.reactivate(1L)).thenReturn(1);
        when(repository.findById(1L)).thenReturn(Optional.of(CatalogItemMother.historiaClinica()));

        assertThat(service.execute(1L).code()).isEqualTo("CLINICAL_HISTORY");
    }

    @Test
    @DisplayName("404 cuando el UPDATE no tocó ninguna fila, y no vuelve a leer")
    void ninguna_fila_actualizada() {
        when(repository.reactivate(1L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 1");

        verify(repository, never()).findById(any());
    }
}
