package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.UpdateBundleComponentCommand;
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
@DisplayName("UpdateBundleComponentService — cambiar la cantidad de una pieza")
class UpdateBundleComponentServiceTest {

    @Mock
    private BundleComponentRepository repository;
    @InjectMocks
    private UpdateBundleComponentService service;

    @Test
    @DisplayName("cambia la cantidad y no reapunta el par de artículos")
    void cambia_la_cantidad() {
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 3L, 2L, 1)));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(service.execute(new UpdateBundleComponentCommand(70L, 3L, 5))).satisfies(dto -> {
            assertThat(dto.quantity()).isEqualTo(5);
            assertThat(dto.bundleItemId()).isEqualTo(3L);
            assertThat(dto.componentItemId()).isEqualTo(2L);
        });
    }

    @Test
    @DisplayName("404 si la pieza no existe")
    void pieza_inexistente() {
        when(repository.findById(70L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new UpdateBundleComponentCommand(70L, 3L, 5)))
                .isInstanceOf(BundleComponentNotFoundException.class)
                .hasMessageContaining("BundleComponent not found: 70");
    }

    @Test
    @DisplayName("404 si la pieza es de otro paquete, y no escribe nada")
    void pieza_de_otro_paquete() {
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 99L, 2L, 1)));

        assertThatThrownBy(() -> service.execute(new UpdateBundleComponentCommand(70L, 3L, 5)))
                .isInstanceOf(BundleComponentNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("propaga la invariante de cantidad sin llegar a guardar")
    void cantidad_invalida() {
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 3L, 2L, 1)));

        assertThatThrownBy(() -> service.execute(new UpdateBundleComponentCommand(70L, 3L, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");

        verify(repository, never()).save(any());
    }
}
