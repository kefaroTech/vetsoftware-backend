package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
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
@DisplayName("CreateRolePermissionService")
class CreateRolePermissionServiceTest {

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
    private CreateRolePermissionService service;

    @Captor
    private ArgumentCaptor<RolePermission> guardado;

    private static CreateRolePermissionCommand comandoConEmpresa() {
        return new CreateRolePermissionCommand(3L, 7L, COMPANY_ID);
    }

    private static CreateRolePermissionCommand comandoSinEmpresa() {
        return new CreateRolePermissionCommand(3L, 7L, null);
    }

    @Nested
    @DisplayName("alta nueva")
    class AltaNueva {

        @Test
        @DisplayName("guarda la asignacion con las referencias resueltas por los puertos")
        void guarda_la_asignacion_con_las_referencias_resueltas() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(RolePermissionMother.activa());

            service.execute(comandoConEmpresa());

            verify(repository).save(guardado.capture());
            RolePermission nuevo = guardado.getValue();
            assertThat(nuevo.getId()).isNull();
            assertThat(nuevo.getRole()).isEqualTo(RolePermissionMother.VETERINARIO);
            assertThat(nuevo.getPermission()).isEqualTo(RolePermissionMother.VER_ANIMALES);
            assertThat(nuevo.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("devuelve el DTO de lo persistido")
        void devuelve_el_dto_de_lo_persistido() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(RolePermissionMother.activa());

            RolePermissionDto dto = service.execute(comandoConEmpresa());

            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.role().code()).isEqualTo("VET");
            assertThat(dto.permission().code()).isEqualTo("ANIMAL_READ");
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("invalida la cache de permisos del rol afectado")
        void invalida_la_cache_del_rol() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(RolePermissionMother.activa());

            service.execute(comandoConEmpresa());

            verify(permissionCachePort).evictByRoleId(3L);
        }

        @Test
        @DisplayName("sin companyId busca las referencias globales, no las de una empresa")
        void sin_company_id_busca_las_referencias_globales() {
            when(roleQueryPort.findById(3L))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findById(7L))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.empty());
            when(repository.save(any())).thenReturn(RolePermissionMother.activa());

            service.execute(comandoSinEmpresa());

            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("reactivacion de una fila desactivada")
    class Reactivacion {

        @Test
        @DisplayName("reactiva en vez de insertar un duplicado")
        void reactiva_en_vez_de_insertar_un_duplicado() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.of(1L));
            when(repository.findById(1L)).thenReturn(Optional.of(RolePermissionMother.activa()));

            RolePermissionDto dto = service.execute(comandoConEmpresa());

            verify(repository).reactivate(1L);
            verify(repository, never()).save(any());
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.enabled()).isTrue();
        }

        @Test
        @DisplayName("invalida la cache tambien cuando reactiva")
        void invalida_la_cache_cuando_reactiva() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.of(1L));
            when(repository.findById(1L)).thenReturn(Optional.of(RolePermissionMother.activa()));

            service.execute(comandoConEmpresa());

            verify(permissionCachePort).evictByRoleId(3L);
        }

        @Test
        @DisplayName("estalla si la fila reactivada desaparece antes de releerla")
        void estalla_si_la_fila_reactivada_desaparece() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));
            when(repository.findDisabledIdByRoleAndPermission(3L, 7L)).thenReturn(Optional.of(1L));
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(RolePermissionNotFoundException.class)
                    .hasMessageContaining("RolePermission not found: 1");

            verifyNoInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("aislamiento por empresa y referencias inexistentes")
    class Aislamiento {

        @Test
        @DisplayName("rol de otra empresa: falla y no escribe nada")
        void rol_de_otra_empresa_falla_sin_escribir() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found: 3");

            verifyNoInteractions(repository, permissionQueryPort, permissionCachePort);
        }

        @Test
        @DisplayName("permiso de otra empresa: falla y no escribe nada")
        void permiso_de_otra_empresa_falla_sin_escribir() {
            when(roleQueryPort.findByIdAndCompanyId(3L, COMPANY_ID))
                    .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
            when(permissionQueryPort.findByIdAndCompanyId(7L, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comandoConEmpresa()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Permission not found: 7");

            verifyNoInteractions(repository, permissionCachePort);
        }
    }
}
