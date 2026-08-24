package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.dto.BundleComponentDto;
import com.vetsoftware.app.catalogitem.application.port.out.BundleComponentRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ListBundleComponentsService — qué trae un paquete")
class ListBundleComponentsServiceTest {

    @Mock
    private BundleComponentRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;
    @InjectMocks
    private ListBundleComponentsService service;

    @Test
    @DisplayName("proyecta las piezas del paquete con su cantidad")
    void proyecta_las_piezas() {
        when(catalogItemRepository.findById(3L))
                .thenReturn(Optional.of(CatalogItemMother.paqueteBasico()));
        when(repository.findAllByBundleItemId(3L))
                .thenReturn(List.of(CatalogItemMother.componente(70L, 3L, 2L, 3)));

        List<BundleComponentDto> dtos = service.listByBundle(3L);

        assertThat(dtos).extracting(BundleComponentDto::quantity).containsExactly(3);
    }

    @Test
    @DisplayName("404 si el paquete no existe")
    void paquete_inexistente() {
        when(catalogItemRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listByBundle(3L))
                .isInstanceOf(CatalogItemNotFoundException.class);

        verifyNoInteractions(repository);
    }
}
