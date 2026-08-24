package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.CreateBundleComponentCommand;
import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import com.vetsoftware.app.catalogitem.domain.BundleComponentAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.InvalidBundleCompositionException;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Las dos comprobaciones que la ficha 4 baja explícitamente a las reglas de
 * código porque un {@code CHECK} de MySQL no puede leer
 * {@code catalog_items.item_type}: el padre tiene que ser {@code BUNDLE} y la
 * pieza no puede serlo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateBundleComponentService — composición de un paquete")
class CreateBundleComponentServiceTest {

    @Mock
    private BundleComponentRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;

    private CreateBundleComponentService service;

    @BeforeEach
    void setUp() {
        service = new CreateBundleComponentService(repository, catalogItemRepository,
                CatalogItemMother.RELOJ);
    }

    private static CreateBundleComponentCommand comando() {
        return new CreateBundleComponentCommand(3L, 2L, 3);
    }

    private void paqueteYPiezaValidos() {
        when(catalogItemRepository.findById(3L))
                .thenReturn(Optional.of(CatalogItemMother.paqueteBasico()));
        when(catalogItemRepository.findById(2L))
                .thenReturn(Optional.of(CatalogItemMother.usuarioExtra()));
    }

    @Test
    @DisplayName("añade la pieza al paquete con su cantidad")
    void anade_la_pieza_al_paquete() {
        paqueteYPiezaValidos();
        when(repository.findAnyByPair(3L, 2L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        BundleComponentDto dto = service.execute(comando());

        ArgumentCaptor<BundleComponent> guardado = ArgumentCaptor.forClass(BundleComponent.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getQuantity()).isEqualTo(3);
        assertThat(guardado.getValue().getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        assertThat(dto.bundleItemId()).isEqualTo(3L);
        assertThat(dto.componentItemId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("409 si el padre no es un BUNDLE: un MODULE no lleva piezas dentro")
    void padre_que_no_es_bundle() {
        when(catalogItemRepository.findById(1L))
                .thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(catalogItemRepository.findById(2L))
                .thenReturn(Optional.of(CatalogItemMother.usuarioExtra()));

        assertThatThrownBy(() -> service.execute(new CreateBundleComponentCommand(1L, 2L, 1)))
                .isInstanceOf(InvalidBundleCompositionException.class)
                .hasMessageContaining("its item type is MODULE");

        verifyNoInteractions(repository);
    }

    /**
     * Los paquetes anidados quedan fuera porque el desglose de una cotización
     * tendría que recorrerse en profundidad y el precio de un pack dejaría de ser
     * la suma de sus líneas.
     */
    @Test
    @DisplayName("409 si la pieza es a su vez un BUNDLE: no se anidan paquetes")
    void pieza_que_es_bundle() {
        when(catalogItemRepository.findById(3L))
                .thenReturn(Optional.of(CatalogItemMother.paqueteBasico()));
        when(catalogItemRepository.findById(4L))
                .thenReturn(Optional.of(CatalogItemMother.conIdYTipo(4L, ItemType.BUNDLE)));

        assertThatThrownBy(() -> service.execute(new CreateBundleComponentCommand(3L, 4L, 1)))
                .isInstanceOf(InvalidBundleCompositionException.class)
                .hasMessageContaining("Nested bundles are not allowed");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("404 si el paquete no existe")
    void paquete_inexistente() {
        when(catalogItemRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 3");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("404 si la pieza no existe")
    void pieza_inexistente() {
        when(catalogItemRepository.findById(3L))
                .thenReturn(Optional.of(CatalogItemMother.paqueteBasico()));
        when(catalogItemRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CatalogItemNotFoundException.class)
                .hasMessageContaining("CatalogItem not found: 2");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("409 si la pieza ya está en el paquete")
    void pieza_duplicada() {
        paqueteYPiezaValidos();
        when(repository.findAnyByPair(3L, 2L)).thenReturn(Optional.of(new LinkStateDto(70L, true)));

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(BundleComponentAlreadyExistsException.class)
                .hasMessageContaining("already contains component 2");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("reactiva la pieza desactivada y le aplica la cantidad nueva")
    void reactiva_la_pieza_desactivada() {
        paqueteYPiezaValidos();
        when(repository.findAnyByPair(3L, 2L))
                .thenReturn(Optional.of(new LinkStateDto(70L, false)));
        when(repository.findById(70L))
                .thenReturn(Optional.of(CatalogItemMother.componente(70L, 3L, 2L, 1)));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        BundleComponentDto dto = service.execute(comando());

        verify(repository).reactivate(70L);
        assertThat(dto.id()).isEqualTo(70L);
        assertThat(dto.quantity()).isEqualTo(3);
    }
}
