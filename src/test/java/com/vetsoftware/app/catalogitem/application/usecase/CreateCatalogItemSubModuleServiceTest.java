package com.vetsoftware.app.catalogitem.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.catalogitem.application.command.CreateCatalogItemSubModuleCommand;
import com.vetsoftware.app.catalogitem.application.dto.CatalogItemSubModuleDto;
import com.vetsoftware.app.catalogitem.application.dto.LinkStateDto;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemRepository;
import com.vetsoftware.app.catalogitem.application.port.out.CatalogItemSubModuleRepository;
import com.vetsoftware.app.catalogitem.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.catalogitem.domain.CatalogItemNotFoundException;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModuleAlreadyExistsException;
import com.vetsoftware.app.catalogitem.testsupport.CatalogItemMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCatalogItemSubModuleService — atar un artículo a un submódulo")
class CreateCatalogItemSubModuleServiceTest {

    @Mock
    private CatalogItemSubModuleRepository repository;
    @Mock
    private CatalogItemRepository catalogItemRepository;
    @Mock
    private SubModuleQueryPort subModuleQueryPort;

    private CreateCatalogItemSubModuleService service;

    @BeforeEach
    void setUp() {
        service = new CreateCatalogItemSubModuleService(repository, catalogItemRepository,
                subModuleQueryPort, CatalogItemMother.RELOJ);
    }

    private static CreateCatalogItemSubModuleCommand comando() {
        return new CreateCatalogItemSubModuleCommand(1L, 50L);
    }

    private void todoExiste() {
        when(catalogItemRepository.findById(1L))
                .thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(subModuleQueryPort.findById(50L))
                .thenReturn(Optional.of(CatalogItemMother.consultas()));
    }

    @Test
    @DisplayName("crea el vínculo con el submódulo resuelto por el puerto")
    void crea_el_vinculo() {
        todoExiste();
        when(repository.findAnyByPair(1L, 50L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        CatalogItemSubModuleDto dto = service.execute(comando());

        ArgumentCaptor<CatalogItemSubModule> guardado = ArgumentCaptor
                .forClass(CatalogItemSubModule.class);
        verify(repository).save(guardado.capture());
        assertThat(guardado.getValue().getSubModule()).isEqualTo(CatalogItemMother.consultas());
        assertThat(guardado.getValue().getCreatedDate()).isEqualTo(CatalogItemMother.CREADO);
        assertThat(dto.subModule().code()).isEqualTo("CONSULTATIONS");
    }

    @Test
    @DisplayName("404 si el artículo no existe, y no consulta el submódulo")
    void articulo_inexistente() {
        when(catalogItemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CatalogItemNotFoundException.class);

        verifyNoInteractions(subModuleQueryPort, repository);
    }

    @Test
    @DisplayName("400 si el submódulo no existe, y no escribe nada")
    void submodulo_inexistente() {
        when(catalogItemRepository.findById(1L))
                .thenReturn(Optional.of(CatalogItemMother.historiaClinica()));
        when(subModuleQueryPort.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SubModule not found: 50");

        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("409 si el vínculo ya está activo")
    void vinculo_ya_activo() {
        todoExiste();
        when(repository.findAnyByPair(1L, 50L))
                .thenReturn(Optional.of(new LinkStateDto(88L, true)));

        assertThatThrownBy(() -> service.execute(comando()))
                .isInstanceOf(CatalogItemSubModuleAlreadyExistsException.class)
                .hasMessageContaining("already opens sub module 50");

        verify(repository, never()).save(any());
        verify(repository, never()).reactivate(any());
    }

    /**
     * El caso que rompería la feature sin este camino: la fila dada de baja sigue
     * ocupando {@code uq_catalog_item_sub_modules} aunque el
     * {@code @SQLRestriction} la esconda, así que un {@code INSERT} chocaría contra
     * una fila que nadie puede ver.
     */
    @Test
    @DisplayName("reactiva el vínculo desactivado en vez de insertar una fila nueva")
    void reactiva_el_vinculo_desactivado() {
        todoExiste();
        when(repository.findAnyByPair(1L, 50L))
                .thenReturn(Optional.of(new LinkStateDto(88L, false)));
        when(repository.findById(88L)).thenReturn(Optional.of(CatalogItemMother.vinculo(88L, 1L)));

        CatalogItemSubModuleDto dto = service.execute(comando());

        verify(repository).reactivate(88L);
        verify(repository, never()).save(any());
        assertThat(dto.id()).isEqualTo(88L);
    }
}
