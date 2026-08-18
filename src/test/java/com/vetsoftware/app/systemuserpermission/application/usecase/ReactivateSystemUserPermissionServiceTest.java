package com.vetsoftware.app.systemuserpermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.port.out.SystemUserPermissionRepository;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermissionNotFoundException;
import com.vetsoftware.app.systemuserpermission.testsupport.SystemUserPermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateSystemUserPermissionService")
class ReactivateSystemUserPermissionServiceTest {

    @Mock
    private SystemUserPermissionRepository repository;

    @InjectMocks
    private ReactivateSystemUserPermissionService service;

    @Test
    @DisplayName("reactiva y devuelve el dto actualizado")
    void reactiva_y_devuelve_el_dto_actualizado() {
        when(repository.reactivate(SystemUserPermissionMother.ID)).thenReturn(1);
        when(repository.findById(SystemUserPermissionMother.ID))
                .thenReturn(Optional.of(SystemUserPermissionMother.asignacionActiva()));

        SystemUserPermissionDto dto = service.execute(SystemUserPermissionMother.ID);

        assertThat(dto.id()).isEqualTo(SystemUserPermissionMother.ID);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("ninguna fila afectada: no busca de nuevo y falla")
    void ninguna_fila_afectada_falla() {
        when(repository.reactivate(999L)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(999L))
                .isInstanceOf(SystemUserPermissionNotFoundException.class)
                .hasMessageContaining("999");

        verify(repository, never()).findById(999L);
    }

    @Test
    @DisplayName("fila afectada pero desaparecida al releer: falla en vez de devolver datos a medias")
    void fila_afectada_pero_desaparecida_al_releer() {
        when(repository.reactivate(SystemUserPermissionMother.ID)).thenReturn(1);
        when(repository.findById(SystemUserPermissionMother.ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(SystemUserPermissionMother.ID))
                .isInstanceOf(SystemUserPermissionNotFoundException.class)
                .hasMessageContaining(String.valueOf(SystemUserPermissionMother.ID));
    }
}
