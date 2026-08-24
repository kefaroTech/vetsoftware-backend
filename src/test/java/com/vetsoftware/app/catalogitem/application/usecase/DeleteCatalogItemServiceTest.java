package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemHasActiveChildrenException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las FK del modelo son {@code ON DELETE RESTRICT}, pero aquí el borrado es
 * lógico: la fila no se va y la base no tiene nada que rechazar. Estas tres
 * guardas son lo único que impide dejar puentes, reglas y paquetes apuntando a
 * un artículo que la aplicación ya no ve.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteCatalogItemService — baja lógica con sus guardas")
class DeleteCatalogItemServiceTest {

    @Mock
    private CatalogItemRepository repository;
    @Mock
    private CatalogItemSubModuleRepository subModuleRepository;
    @Mock
    private CatalogItemDependencyRepository dependencyRepository;
    @Mock
    private BundleComponentRepository bundleComponentRepository;
    @InjectMocks
    private DeleteCatalogItemService service;

    private void elArticuloExiste() {
        when(repository.findById(1L)).thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
    }

    @Test
    @DisplayName("da de baja el artículo sin hijos activos")
    void da_de_baja_el_articulo_sin_hijos() {
        elArticuloExiste();
        when(subModuleRepository.existsActiveByCatalogItemId(1L)).thenReturn(false);
        when(dependencyRepository.existsActiveInvolving(1L)).thenReturn(false);
        when(bundleComponentRepository.existsActiveInvolving(1L)).thenReturn(false);

        service.execute(1L);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("404 si el artículo no existe, y no consulta ninguna tabla hija")
    void articulo_inexistente() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 1");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("409 si todavía abre algún submódulo")
    void con_submodulos_activos() {
        elArticuloExiste();
        when(subModuleRepository.existsActiveByCatalogItemId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(CatalogItemHasActiveChildrenException.class)
                .hasMessageContaining("catalogItemSubModule");

        verify(repository, never()).delete(any());
    }

    /**
     * Alcanza a las reglas donde el artículo es sujeto <em>y</em> a aquellas donde
     * es el relacionado: si solo se mirara el primer sentido, retirar un artículo
     * dejaría vivos los {@code REQUIRES} que apuntan a él y el configurador
     * seguiría exigiéndolo.
     */
    @Test
    @DisplayName("409 si sigue apareciendo en alguna regla del configurador")
    void con_dependencias_activas() {
        elArticuloExiste();
        when(subModuleRepository.existsActiveByCatalogItemId(1L)).thenReturn(false);
        when(dependencyRepository.existsActiveInvolving(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(CatalogItemHasActiveChildrenException.class)
                .hasMessageContaining("catalogItemDependency");

        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("409 si es paquete de algo o pieza de algún paquete vivo")
    void con_componentes_activos() {
        elArticuloExiste();
        when(subModuleRepository.existsActiveByCatalogItemId(1L)).thenReturn(false);
        when(dependencyRepository.existsActiveInvolving(1L)).thenReturn(false);
        when(bundleComponentRepository.existsActiveInvolving(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(1L))
                .isInstanceOf(CatalogItemHasActiveChildrenException.class)
                .hasMessageContaining("bundleComponent");

        verify(repository, never()).delete(any());
    }
}
