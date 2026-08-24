package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCatalogItemDependencyService — retirar una regla")
class DeleteCatalogItemDependencyServiceTest {

    @Mock
    private CatalogItemDependencyRepository repository;
    @InjectMocks
    private DeleteCatalogItemDependencyService service;

    @Test
    @DisplayName("da de baja la regla del artículo de la ruta")
    void da_de_baja_la_regla() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.REQUIRES)));

        service.execute(1L, 9L);

        verify(repository).delete(9L);
    }

    @Test
    @DisplayName("404 si la regla no existe")
    void regla_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, 9L))
                .isInstanceOf(CatalogItemDependencyNotFoundException.class)
                .hasMessageContaining("CatalogItemDependency not found: 9");
    }

    @Test
    @DisplayName("404 si la regla cuelga de otro artículo, y no borra nada")
    void regla_de_otro_articulo() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 99L, 2L, RelationType.REQUIRES)));

        assertThatThrownBy(() -> service.execute(1L, 9L))
                .isInstanceOf(CatalogItemDependencyNotFoundException.class);

        verify(repository, never()).delete(any());
    }
}
