package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RolePermissionNotFoundException;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateRolePermissionService")
class UpdateRolePermissionServiceTest {

    private static final Long COMPANY_ID = RolePermissionMother.COMPANY_ID;

    @Mock
    private RolePermissionRepository repository;
    @Mock
    private RoleQueryPort roleQueryPort;
    @Mock
    private PermissionQueryPort permissionQueryPort;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private UpdateRolePermissionService service;

    @Captor
    private ArgumentCaptor<RolePermission> guardado;

    /** Cambia el rol 3 → 4 y el permiso 7 → 8. */
    private static UpdateRolePermissionCommand comandoQueMueveElRol() {
        return new UpdateRolePermissionCommand(1L, 4L, 8L, COMPANY_ID);
    }

    /** Mantiene el rol 3 y solo cambia el permiso. */
    private static UpdateRolePermissionCommand comandoQueMantieneElRol() {
        return new UpdateRolePermissionCommand(1L, 3L, 8L, COMPANY_ID);
    }

    @Nested
    @DisplayName("camino feliz")
    class CaminoFeliz {

        @Test
        @DisplayName("guarda la asignacion con el rol y el permiso nuevos")
        void guarda_con_el_rol_y_el_permiso_nuevos() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(4L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.RECEPCION));
            when(permissionQueryPort.findByIdAndCompanyId(8L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.CREAR_ANIMALES));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoQueMueveElRol());

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getRole()).isEqualTo(RolePermissionMother.RECEPCION);
            assertThat(guardado.getValue().getPermission())
                    .isEqualTo(RolePermissionMother.CREAR_ANIMALES);
            assertThat(guardado.getValue().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("devuelve el DTO de lo guardado")
        void devuelve_el_dto_de_lo_guardado() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(4L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.RECEPCION));
            when(permissionQueryPort.findByIdAndCompanyId(8L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.CREAR_ANIMALES));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RolePermissionDto dto = service.execute(comandoQueMueveElRol());

            assertThat(dto.role().id()).isEqualTo(4L);
            assertThat(dto.permission().id()).isEqualTo(8L);
        }
    }

    @Nested
    @DisplayName("invalidacion de cache")
    class Cache {

        @Test
        @DisplayName("al mover la asignacion de rol invalida la cache de ambos roles")
        void invalida_la_cache_de_ambos_roles() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(4L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.RECEPCION));
            when(permissionQueryPort.findByIdAndCompanyId(8L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.CREAR_ANIMALES));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoQueMueveElRol());

            verify(permissionCachePort).evictByRoleId(3L);
            verify(permissionCachePort).evictByRoleId(4L);
        }

        @Test
        @DisplayName("si el rol no cambia solo invalida una vez")
        void si_el_rol_no_cambia_solo_invalida_una_vez() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(8L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.CREAR_ANIMALES));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comandoQueMantieneElRol());

            verify(permissionCachePort).evictByRoleId(3L);
            verifyNoMoreInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("errores")
    class Errores {

        @Test
        @DisplayName("asignacion de otra empresa: 404 de dominio y cero escrituras")
        void asignacion_de_otra_empresa() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoQueMueveElRol()))
                    .isInstanceOf(RolePermissionNotFoundException.class)
                    .hasMessageContaining("RolePermission not found: 1");

            verifyNoInteractions(roleQueryPort, permissionQueryPort, permissionCachePort);
        }

        @Test
        @DisplayName("rol destino inexistente: no guarda ni invalida cache")
        void rol_destino_inexistente() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(4L, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoQueMueveElRol()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found: 4");

            verifyNoInteractions(permissionQueryPort, permissionCachePort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("permiso destino inexistente: no guarda ni invalida cache")
        void permiso_destino_inexistente() {
            when(repository.findByIdAndCompanyId(1L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.activa()));
            when(roleQueryPort.findByIdAndCompanyId(4L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.RECEPCION));
            when(permissionQueryPort.findByIdAndCompanyId(8L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoQueMueveElRol()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Permission not found: 8");

            verifyNoInteractions(permissionCachePort);
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin companyId resuelve la asignacion sin filtro de empresa")
        void sin_company_id_resuelve_sin_filtro() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(new UpdateRolePermissionCommand(1L, 4L, 8L, null)))
                    .isInstanceOf(RolePermissionNotFoundException.class);
        }
    }
}
