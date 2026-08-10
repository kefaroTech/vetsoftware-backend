package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteRolePermissionService")
class DeleteRolePermissionServiceTest {

    private static final Long COMPANY_ID = RolePermissionMother.COMPANY_ID;

    @Mock
    private RolePermissionRepository repository;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private DeleteRolePermissionService service;

    @Test
    @DisplayName("borra la asignacion de la propia empresa")
    void borra_la_asignacion_de_la_propia_empresa() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                .thenReturn(Optional.of(RolePermissionMother.activa()));

        service.execute(1L, COMPANY_ID);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("invalida la cache del rol despues de borrar")
    void invalida_la_cache_del_rol_despues_de_borrar() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                .thenReturn(Optional.of(RolePermissionMother.activa()));

        service.execute(1L, COMPANY_ID);

        InOrder orden = inOrder(repository, permissionCachePort);
        orden.verify(repository).delete(1L);
        orden.verify(permissionCachePort).evictByRoleId(3L);
    }

    @Test
    @DisplayName("sin companyId resuelve la asignacion sin filtro de empresa")
    void sin_company_id_resuelve_sin_filtro() {
        when(repository.findById(1L)).thenReturn(Optional.of(RolePermissionMother.activa()));

        service.execute(1L, null);

        verify(repository).delete(1L);
    }

    @Test
    @DisplayName("asignacion de otra empresa: falla y no borra ni invalida cache")
    void asignacion_de_otra_empresa_no_borra() {
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                .isInstanceOf(RolePermissionNotFoundException.class)
                .hasMessageContaining("RolePermission not found: 1");

        verify(repository, never()).delete(any());
        verifyNoInteractions(permissionCachePort);
    }
}
