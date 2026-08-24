package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemDependencyCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemDependencyDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemDependencyRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyAlreadyExistsException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemDependencyCycleException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.DependencyEdge;
import com.vetsoftware.app.catalogitem.domain.ItemType;
import com.vetsoftware.app.catalogitem.domain.RelationType;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La regla R16 vista desde el caso de uso: aquí es donde un ciclo indirecto
 * tiene que convertirse en un rechazo, y donde hay que demostrar que <strong>no
 * se escribe nada</strong> cuando lo hay.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCatalogItemDependencyService — alta de una regla del configurador")
class CreateCatalogItemDependencyServiceTest {

    @Mock
    private CatalogItemDependencyRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;

    private CreateCatalogItemDependencyService service;

    @BeforeEach
    void setUp() {
        service = new CreateCatalogItemDependencyService(repository, catalogItemRepository,
                CatalogItemMother.RELOJ);
    }

    private void existenLosArticulos(Long... ids) {
        for (Long id : ids) {
            when(catalogItemRepository.findById(id))
                    .thenReturn(Optional.of(CatalogItemMother.conIdYTipo(id, ItemType.MODULE)));
        }
    }

    private static CreateCatalogItemDependencyCommand comando(RelationType relationType) {
        return new CreateCatalogItemDependencyCommand(1L, 2L, relationType, "Necesitas caja");
    }

    @Nested
    @DisplayName("Detección de ciclos (R16)")
    class Ciclos {

        @Test
        @DisplayName("rechaza el REQUIRES que cierra un ciclo indirecto y no escribe nada")
        void rechaza_el_requires_que_cierra_un_ciclo() {
            existenLosArticulos(1L, 2L);
            when(repository.findAllRequiresEdges())
                    .thenReturn(List.of(new DependencyEdge(2L, 3L), new DependencyEdge(3L, 1L)));

            assertThatThrownBy(() -> service.execute(comando(RelationType.REQUIRES)))
                    .isInstanceOf(CatalogItemDependencyCycleException.class)
                    .hasMessageContaining("1 > 2 > 3 > 1");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la excepción lleva la ruta del ciclo, no solo el aviso de que lo hay")
        void la_excepcion_lleva_la_ruta_del_ciclo() {
            existenLosArticulos(1L, 2L);
            when(repository.findAllRequiresEdges()).thenReturn(List.of(new DependencyEdge(2L, 1L)));

            assertThatThrownBy(() -> service.execute(comando(RelationType.REQUIRES)))
                    .isInstanceOf(CatalogItemDependencyCycleException.class)
                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories
                            .type(CatalogItemDependencyCycleException.class))
                    .extracting(CatalogItemDependencyCycleException::getCycle)
                    .isEqualTo(List.of(1L, 2L, 1L));
        }

        @Test
        @DisplayName("acepta el REQUIRES que no cierra ciclo")
        void acepta_el_requires_que_no_cierra_ciclo() {
            existenLosArticulos(1L, 2L);
            when(repository.findAllRequiresEdges()).thenReturn(List.of(new DependencyEdge(2L, 3L)));
            when(repository.findAnyByTriple(1L, 2L, RelationType.REQUIRES))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

            CatalogItemDependencyDto dto = service.execute(comando(RelationType.REQUIRES));

            assertThat(dto.catalogItemId()).isEqualTo(1L);
            assertThat(dto.relatedItemId()).isEqualTo(2L);
            assertThat(dto.relationType()).isEqualTo(RelationType.REQUIRES);
        }

        @Test
        @DisplayName("un RECOMMENDS ni siquiera consulta el grafo: no arrastra, no puede ciclar")
        void un_recommends_no_consulta_el_grafo() {
            existenLosArticulos(1L, 2L);
            when(repository.findAnyByTriple(1L, 2L, RelationType.RECOMMENDS))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

            service.execute(comando(RelationType.RECOMMENDS));

            verify(repository, never()).findAllRequiresEdges();
        }

        /**
         * Reactivar un arco desactivado lo devuelve al grafo igual que insertarlo, y el
         * grafo que se carga solo trae los activos. Si el detector corriera después de
         * la rama de duplicados, este ciclo entraría sin que nadie lo viera.
         */
        @Test
        @DisplayName("reactivar un REQUIRES desactivado también pasa por el detector de ciclos")
        void reactivar_un_requires_tambien_pasa_por_el_detector() {
            existenLosArticulos(1L, 2L);
            when(repository.findAllRequiresEdges()).thenReturn(List.of(new DependencyEdge(2L, 1L)));

            assertThatThrownBy(() -> service.execute(comando(RelationType.REQUIRES)))
                    .isInstanceOf(CatalogItemDependencyCycleException.class);

            verify(repository, never()).reactivate(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones y duplicados")
    class Validaciones {

        @Test
        @DisplayName("404 si el artículo sujeto no existe, y no toca el repositorio de reglas")
        void articulo_sujeto_inexistente() {
            when(catalogItemRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(RelationType.REQUIRES)))
                    .isInstanceOf(CatalogItemNotFoundException.class)
                    .hasMessageContaining("CatalogItem not found: 1");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("404 si el artículo relacionado no existe")
        void articulo_relacionado_inexistente() {
            existenLosArticulos(1L);
            when(catalogItemRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(RelationType.REQUIRES)))
                    .isInstanceOf(CatalogItemNotFoundException.class)
                    .hasMessageContaining("CatalogItem not found: 2");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("400 si la regla apunta al mismo artículo: es el ciclo trivial")
        void regla_a_si_misma() {
            when(catalogItemRepository.findById(1L))
                    .thenReturn(Optional.of(CatalogItemMother.conIdYTipo(1L, ItemType.MODULE)));

            assertThatThrownBy(() -> service.execute(
                    new CreateCatalogItemDependencyCommand(1L, 1L, RelationType.REQUIRES, null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot depend on itself");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("409 si la terna ya existe activa")
        void terna_ya_existente() {
            existenLosArticulos(1L, 2L);
            when(repository.findAnyByTriple(1L, 2L, RelationType.RECOMMENDS))
                    .thenReturn(Optional.of(new LinkStateDto(77L, true)));

            assertThatThrownBy(() -> service.execute(comando(RelationType.RECOMMENDS)))
                    .isInstanceOf(CatalogItemDependencyAlreadyExistsException.class)
                    .hasMessageContaining("1 RECOMMENDS 2");

            verify(repository, never()).save(any());
            verify(repository, never()).reactivate(any());
        }

        /**
         * La fila dada de baja sigue ocupando la terna única aunque el
         * {@code @SQLRestriction} la esconda: insertar otra chocaría contra algo
         * invisible.
         */
        @Test
        @DisplayName("reactiva la terna desactivada en vez de insertar una fila nueva")
        void reactiva_la_terna_desactivada() {
            existenLosArticulos(1L, 2L);
            when(repository.findAnyByTriple(1L, 2L, RelationType.RECOMMENDS))
                    .thenReturn(Optional.of(new LinkStateDto(77L, false)));
            when(repository.findById(77L)).thenReturn(Optional
                    .of(CatalogItemMother.dependencia(77L, 1L, 2L, RelationType.RECOMMENDS)));
            when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

            CatalogItemDependencyDto dto = service.execute(comando(RelationType.RECOMMENDS));

            verify(repository).reactivate(77L);
            assertThat(dto.id()).isEqualTo(77L);

            ArgumentCaptor<CatalogItemDependency> guardada = ArgumentCaptor
                    .forClass(CatalogItemDependency.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getNote()).isEqualTo("Necesitas caja");
        }

        @Test
        @DisplayName("guarda la nota y el sentido tal como llegaron en el comando")
        void guarda_lo_que_llego_en_el_comando() {
            existenLosArticulos(1L, 2L);
            when(repository.findAnyByTriple(eq(1L), eq(2L), eq(RelationType.EXCLUDES)))
                    .thenReturn(Optional.empty());
            when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

            service.execute(new CreateCatalogItemDependencyCommand(1L, 2L, RelationType.EXCLUDES,
                    "No se pueden vender juntos"));

            ArgumentCaptor<CatalogItemDependency> guardada = ArgumentCaptor
                    .forClass(CatalogItemDependency.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getRelationType()).isEqualTo(RelationType.EXCLUDES);
            assertThat(guardada.getValue().getNote()).isEqualTo("No se pueden vender juntos");
            assertThat(guardada.getValue().getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        }
    }
}
