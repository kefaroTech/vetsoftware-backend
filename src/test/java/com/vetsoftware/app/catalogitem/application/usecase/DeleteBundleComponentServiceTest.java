package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponentNotFoundException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteBundleComponentService — quitar una pieza del paquete")
class DeleteBundleComponentServiceTest {

    @Mock
    private BundleComponentRepository repository;
    @InjectMocks
    private DeleteBundleComponentService service;

    @Test
    @DisplayName("da de baja la pieza del paquete de la ruta")
    void da_de_baja_la_pieza() {
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 3L, 2L, 1)));

        service.execute(3L, 70L);

        verify(repository).delete(70L);
    }

    @Test
    @DisplayName("404 si la pieza no existe")
    void pieza_inexistente() {
        when(repository.findById(70L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(3L, 70L))
                .isInstanceOf(BundleComponentNotFoundException.class)
                .hasMessageContaining("BundleComponent not found: 70");
    }

    @Test
    @DisplayName("404 si la pieza es de otro paquete, y no borra nada")
    void pieza_de_otro_paquete() {
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 99L, 2L, 1)));

        assertThatThrownBy(() -> service.execute(3L, 70L))
                .isInstanceOf(BundleComponentNotFoundException.class);

        verify(repository, never()).delete(any());
    }
}
