package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.UpdateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyNotFoundException;
import com.vetsoftware.app.catalogitem.domain.DependencyEdge;
import com.vetsoftware.app.catalogitem.domain.RelationType;
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
@DisplayName("UpdateCatalogItemDependencyService — edición de una regla del configurador")
class UpdateCatalogItemDependencyServiceTest {

    @Mock
    private CatalogItemDependencyRepository repository;
    @InjectMocks
    private UpdateCatalogItemDependencyService service;

    @Test
    @DisplayName("pasar de RECOMMENDS a REQUIRES pasa por el detector: añade un arco al grafo")
    void pasar_a_requires_dispara_el_detector() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.RECOMMENDS)));
        when(repository.findAllRequiresEdges()).thenReturn(List.of(new DependencyEdge(2L, 1L)));

        assertThatThrownBy(() -> service.execute(
                new UpdateCatalogItemDependencyCommand(9L, 1L, RelationType.REQUIRES, null)))
                .isInstanceOf(CatalogItemDependencyCycleException.class)
                .hasMessageContaining("1 > 2 > 1");

        verify(repository, never()).save(any());
    }

    /**
     * El falso positivo que se evitó: el arco ya está en el grafo que devuelve el
     * repositorio, así que el detector lo encontraría a través de sí mismo y
     * bloquearía editar la nota de cualquier dependencia legítima.
     */
    @Test
    @DisplayName("editar un REQUIRES que ya lo era no consulta el grafo, y por eso no falsea")
    void editar_un_requires_que_ya_lo_era_no_consulta_el_grafo() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.REQUIRES)));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        CatalogItemDependencyDto dto = service.execute(new UpdateCatalogItemDependencyCommand(9L,
                1L, RelationType.REQUIRES, "Nota nueva"));

        verify(repository, never()).findAllRequiresEdges();
        assertThat(dto.note()).isEqualTo("Nota nueva");
        assertThat(dto.relationType()).isEqualTo(RelationType.REQUIRES);
    }

    @Test
    @DisplayName("bajar de REQUIRES a RECOMMENDS quita un arco: tampoco hay nada que comprobar")
    void bajar_de_requires_no_consulta_el_grafo() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.REQUIRES)));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        service.execute(
                new UpdateCatalogItemDependencyCommand(9L, 1L, RelationType.RECOMMENDS, null));

        verify(repository, never()).findAllRequiresEdges();
    }

    @Test
    @DisplayName("404 cuando la regla no existe")
    void regla_inexistente() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                new UpdateCatalogItemDependencyCommand(9L, 1L, RelationType.REQUIRES, null)))
                .isInstanceOf(CatalogItemDependencyNotFoundException.class)
                .hasMessageContaining("CatalogItemDependency not found: 9");
    }

    /**
     * Un id escrito a mano en la URL no puede editar la regla de otro artículo. Se
     * responde como inexistente, que es lo que es para quien pregunta por esa ruta.
     */
    @Test
    @DisplayName("404 cuando la regla no cuelga del artículo de la ruta, y no escribe nada")
    void regla_de_otro_articulo() {
        when(repository.findById(9L)).thenReturn(
                Optional.of(CatalogItemMother.dependencia(9L, 1L, 2L, RelationType.REQUIRES)));

        assertThatThrownBy(() -> service.execute(
                new UpdateCatalogItemDependencyCommand(9L, 99L, RelationType.REQUIRES, null)))
                .isInstanceOf(CatalogItemDependencyNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
