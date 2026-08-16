package com.vetsoftware.app.rolepermission.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionCachePort;
import com.vetsoftware.app.rolepermission.application.port.out.PermissionQueryPort;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository;
import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository.DisabledRolePermissionLookup;
import com.vetsoftware.app.rolepermission.application.port.out.RoleQueryPort;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.testsupport.RolePermissionMother;
import java.util.Collection;
import java.util.List;
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
@DisplayName("SyncRolePermissionsService — reconciliacion del set de permisos de un rol")
class SyncRolePermissionsServiceTest {

    private static final Long ROLE_ID = 3L;
    private static final Long COMPANY_ID = 9L;

    @Mock
    private RolePermissionRepository repository;
    @Mock
    private RoleQueryPort roleQueryPort;
    @Mock
    private PermissionQueryPort permissionQueryPort;
    @Mock
    private PermissionCachePort permissionCachePort;

    @InjectMocks
    private SyncRolePermissionsService service;

    @Captor
    private ArgumentCaptor<List<Long>> idsBorrados;
    @Captor
    private ArgumentCaptor<List<RolePermission>> nuevos;
    @Captor
    private ArgumentCaptor<Collection<Long>> idsReactivados;

    private void elRolExiste() {
        when(roleQueryPort.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                .thenReturn(Optional.of(RolePermissionMother.VETERINARIO));
    }

    @Nested
    @DisplayName("validaciones de entrada")
    class Validaciones {

        @Test
        @DisplayName("roleId nulo falla antes de tocar nada")
        void role_id_nulo_falla_antes_de_tocar_nada() {
            assertThatThrownBy(() -> service
                    .execute(new SyncRolePermissionsCommand(null, List.of(7L), COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("roleId is required");

            verifyNoInteractions(repository, roleQueryPort, permissionQueryPort,
                    permissionCachePort);
        }

        @Test
        @DisplayName("rol inexistente falla y no escribe nada")
        void rol_inexistente_falla_sin_escribir() {
            when(roleQueryPort.findByIdAndCompanyId(ROLE_ID, COMPANY_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Role not found: 3");

            verifyNoInteractions(repository, permissionQueryPort, permissionCachePort);
        }
    }

    @Nested
    @DisplayName("altas")
    class Altas {

        @Test
        @DisplayName("crea las asignaciones que no existian, ni activas ni desactivadas")
        void crea_las_que_no_existian() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(),
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));
            when(repository.findDisabledByRoleAndPermissions(eq(ROLE_ID), anyCollection()))
                    .thenReturn(List.of());
            when(permissionQueryPort.findById(7L))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            verify(repository).saveAll(nuevos.capture());
            assertThat(nuevos.getValue()).singleElement()
                    .satisfies(rp -> assertThat(rp.getPermission())
                            .isEqualTo(RolePermissionMother.VER_ANIMALES));
            assertThat(nuevos.getValue().get(0).getRole())
                    .isEqualTo(RolePermissionMother.VETERINARIO);
        }

        @Test
        @DisplayName("un permiso inexistente aborta la sincronizacion")
        void un_permiso_inexistente_aborta() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(List.of());
            when(repository.findDisabledByRoleAndPermissions(eq(ROLE_ID), anyCollection()))
                    .thenReturn(List.of());
            when(permissionQueryPort.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(99L), COMPANY_ID)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Permission not found: 99");

            verify(repository, never()).saveAll(anyList());
            verifyNoInteractions(permissionCachePort);
        }
    }

    @Nested
    @DisplayName("reactivaciones")
    class Reactivaciones {

        @Test
        @DisplayName("reactiva la fila desactivada en vez de insertar un duplicado")
        void reactiva_en_vez_de_insertar_un_duplicado() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(),
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));
            when(repository.findDisabledByRoleAndPermissions(eq(ROLE_ID), anyCollection()))
                    .thenReturn(List.of(new DisabledRolePermissionLookup(1L, 7L)));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            verify(repository).reactivateAllByIds(idsReactivados.capture());
            assertThat(idsReactivados.getValue()).containsExactly(1L);
            verify(repository, never()).saveAll(anyList());
            verifyNoInteractions(permissionQueryPort);
        }

        @Test
        @DisplayName("mezcla reactivacion y alta: solo consulta el permiso que hay que crear")
        void mezcla_reactivacion_y_alta() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(),
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));
            when(repository.findDisabledByRoleAndPermissions(eq(ROLE_ID), anyCollection()))
                    .thenReturn(List.of(new DisabledRolePermissionLookup(1L, 7L)));
            when(permissionQueryPort.findById(8L))
                    .thenReturn(Optional.of(RolePermissionMother.CREAR_ANIMALES));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L, 8L), COMPANY_ID));

            verify(repository).reactivateAllByIds(idsReactivados.capture());
            assertThat(idsReactivados.getValue()).containsExactly(1L);
            verify(repository).saveAll(nuevos.capture());
            assertThat(nuevos.getValue()).singleElement()
                    .satisfies(rp -> assertThat(rp.getPermission().id()).isEqualTo(8L));
        }
    }

    @Nested
    @DisplayName("bajas")
    class Bajas {

        @Test
        @DisplayName("borra las asignaciones que ya no estan en la lista deseada")
        void borra_las_que_sobran() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES),
                            RolePermissionMother.conId(2L, RolePermissionMother.CREAR_ANIMALES)),
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            verify(repository).deleteAllByIds(idsBorrados.capture());
            assertThat(idsBorrados.getValue()).containsExactly(2L);
            verify(repository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("una lista vacia de permisos deja el rol sin ninguna asignacion")
        void lista_vacia_deja_el_rol_sin_asignaciones() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)),
                    List.of());

            List<RolePermissionDto> resultado = service
                    .execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(), COMPANY_ID));

            verify(repository).deleteAllByIds(idsBorrados.capture());
            assertThat(idsBorrados.getValue()).containsExactly(1L);
            assertThat(resultado).isEmpty();
        }

        @Test
        @DisplayName("permissionIds nulo se trata como lista vacia y borra todo")
        void permission_ids_nulo_se_trata_como_lista_vacia() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)),
                    List.of());

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, null, COMPANY_ID));

            verify(repository).deleteAllByIds(idsBorrados.capture());
            assertThat(idsBorrados.getValue()).containsExactly(1L);
            verifyNoInteractions(permissionQueryPort);
        }
    }

    @Nested
    @DisplayName("idempotencia y resultado")
    class IdempotenciaYResultado {

        @Test
        @DisplayName("si el set deseado coincide con el actual no borra ni crea nada")
        void si_coincide_no_borra_ni_crea() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            verify(repository, never()).deleteAllByIds(anyList());
            verify(repository, never()).saveAll(anyList());
            verify(repository, never()).reactivateAllByIds(anyCollection());
        }

        @Test
        @DisplayName("los ids duplicados en la peticion no generan altas repetidas")
        void los_ids_duplicados_no_generan_altas_repetidas() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(List.of(),
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));
            when(repository.findDisabledByRoleAndPermissions(eq(ROLE_ID), anyCollection()))
                    .thenReturn(List.of());
            when(permissionQueryPort.findById(7L))
                    .thenReturn(Optional.of(RolePermissionMother.VER_ANIMALES));

            service.execute(
                    new SyncRolePermissionsCommand(ROLE_ID, List.of(7L, 7L, 7L), COMPANY_ID));

            verify(repository).saveAll(nuevos.capture());
            assertThat(nuevos.getValue()).hasSize(1);
        }

        @Test
        @DisplayName("devuelve el estado final releido del repositorio, no el calculado")
        void devuelve_el_estado_final_releido() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));

            List<RolePermissionDto> resultado = service
                    .execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            assertThat(resultado).singleElement().extracting(RolePermissionDto::id).isEqualTo(1L);
        }

        @Test
        @DisplayName("siempre invalida la cache de permisos del rol sincronizado")
        void siempre_invalida_la_cache_del_rol() {
            elRolExiste();
            when(repository.findAllByRoleId(ROLE_ID)).thenReturn(
                    List.of(RolePermissionMother.conId(1L, RolePermissionMother.VER_ANIMALES)));

            service.execute(new SyncRolePermissionsCommand(ROLE_ID, List.of(7L), COMPANY_ID));

            verify(permissionCachePort).evictByRoleId(ROLE_ID);
        }
    }
}
