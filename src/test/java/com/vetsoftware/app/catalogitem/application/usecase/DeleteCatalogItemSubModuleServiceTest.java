package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleNotFoundException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCatalogItemSubModuleService — retirar un vínculo")
class DeleteCatalogItemSubModuleServiceTest {

    @Mock
    private CatalogItemSubModuleRepository repository;
    @InjectMocks
    private DeleteCatalogItemSubModuleService service;

    @Test
    @DisplayName("da de baja el vínculo del artículo de la ruta")
    void da_de_baja_el_vinculo() {
        when(repository.findById(5L)).thenReturn(Optional.of(CatalogItemMother.vinculo(5L, 1L)));

        service.execute(1L, 5L);

        verify(repository).delete(5L);
    }

    @Test
    @DisplayName("404 si el vínculo no existe")
    void vinculo_inexistente() {
        when(repository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, 5L))
                .isInstanceOf(CatalogItemSubModuleNotFoundException.class)
                .hasMessageContaining("CatalogItemSubModule not found: 5");
    }

    /**
     * Un id escrito a mano en la URL no puede retirar el vínculo de otro artículo:
     * para quien pregunta por esa ruta, ese vínculo no existe.
     */
    @Test
    @DisplayName("404 si el vínculo cuelga de otro artículo, y no borra nada")
    void vinculo_de_otro_articulo() {
        when(repository.findById(5L)).thenReturn(Optional.of(CatalogItemMother.vinculo(5L, 99L)));

        assertThatThrownBy(() -> service.execute(1L, 5L))
                .isInstanceOf(CatalogItemSubModuleNotFoundException.class);

        verify(repository, never()).delete(any());
    }
}
