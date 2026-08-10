package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateRolePermissionService")
class ReactivateRolePermissionServiceTest {

    private static final Long COMPANY_ID = RolePermissionMother.COMPANY_ID;

    @Mock
    private RolePermissionRepository repository;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private ReactivateRolePermissionService service;

    @Test
    @DisplayName("reactiva acotando por empresa y devuelve la asignacion ya activa")
    void reactiva_acotando_por_empresa() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                .thenReturn(Optional.of(RolePermissionMother.activa()));

        RolePermissionDto dto = service.execute(1L, COMPANY_ID);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("invalida la cache del rol reactivado")
    void invalida_la_cache_del_rol_reactivado() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                .thenReturn(Optional.of(RolePermissionMother.activa()));

        service.execute(1L, COMPANY_ID);

        verify(permissionCachePort).evictByRoleId(3L);
    }

    @Test
    @DisplayName("sin companyId reactiva por id a secas")
    void sin_company_id_reactiva_por_id() {
        when(repository.reactivate(1L)).thenReturn(1);
        when(repository.findById(1L)).thenReturn(Optional.of(RolePermissionMother.activa()));

        RolePermissionDto dto = service.execute(1L, null);

        assertThat(dto.id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("si el UPDATE no toca filas la asignacion no es de esta empresa: 404 sin cache")
    void si_el_update_no_toca_filas_falla() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(0);

        assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                .isInstanceOf(RolePermissionNotFoundException.class)
                .hasMessageContaining("RolePermission not found: 1");

        verifyNoInteractions(permissionCachePort);
    }

    @Test
    @DisplayName("si la relectura vuelve vacia falla sin invalidar la cache")
    void si_la_relectura_vuelve_vacia_falla() {
        when(repository.reactivate(1L, COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(1L, COMPANY_ID))
                .isInstanceOf(RolePermissionNotFoundException.class);

        verifyNoInteractions(permissionCachePort);
    }
}
