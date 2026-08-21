package com.vetsoftware.app.systemuserpermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.systemuserpermission.domain.SystemPermissionRef;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserPermission;
import com.vetsoftware.app.systemuserpermission.domain.SystemUserRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Rodaja de persistencia de la asignacion permiso-usuario de sistema contra
 * MySQL real.
 *
 * <p>
 * Lo que no ve un test de mapper ni uno de servicio: el {@code @EntityGraph}
 * que evita el N+1 al hidratar usuario y permiso, la unicidad
 * {@code (system_user_id, system_permission_id)} que impone el indice de la
 * migracion, y que {@code @SQLDelete}/{@code @SQLRestriction} convierten el
 * borrado y el filtro "deshabilitado" en comportamiento del motor, no de este
 * codigo Java.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaSystemUserPermissionRepository — asignaciones contra MySQL real")
class SystemUserPermissionPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaSystemUserPermissionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long usuarioId;
    private Long permisoId;
    private Long otroPermisoId;

    @BeforeEach
    void sembrarUsuarioYPermisos() {
        usuarioId = insertarSystemUser("op-admin");
        permisoId = insertarSystemPermission("Gestionar Reportes", "reports.manage");
        otroPermisoId = insertarSystemPermission("Gestionar Facturas", "billing.manage");
    }

    private Long insertarSystemUser(String code) {
        entityManager
                .createNativeQuery("INSERT INTO system_users (code, hash_password, created_date, "
                        + "enabled) VALUES (:code, 'hash', NOW(), true)")
                .setParameter("code", code).executeUpdate();
        entityManager.flush();
        Long id = ((Number) entityManager
                .createNativeQuery("SELECT id FROM system_users WHERE code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
        entityManager.clear();
        return id;
    }

    private Long insertarSystemPermission(String name, String code) {
        entityManager
                .createNativeQuery("INSERT INTO system_permissions (name, code, "
                        + "created_date, enabled) VALUES (:name, :code, NOW(), true)")
                .setParameter("name", name).setParameter("code", code).executeUpdate();
        entityManager.flush();
        Long id = ((Number) entityManager
                .createNativeQuery("SELECT id FROM system_permissions WHERE code = :code")
                .setParameter("code", code).getSingleResult()).longValue();
        entityManager.clear();
        return id;
    }

    private SystemUserPermission asignacion(Long systemUserId, Long systemPermissionId) {
        return SystemUserPermission.create(new SystemUserRef(systemUserId, "op-admin"),
                new SystemPermissionRef(systemPermissionId, "Gestionar Reportes",
                        "reports.manage"));
    }

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar asigna id y releer trae usuario y permiso hidratados")
        void guardar_asigna_id_y_releer_hidrata_las_asociaciones() {
            SystemUserPermission guardada = repository.save(asignacion(usuarioId, permisoId));

            assertThat(guardada.getId()).isNotNull();

            SystemUserPermission leida = repository.findById(guardada.getId()).orElseThrow();
            assertThat(leida.getSystemUser().id()).isEqualTo(usuarioId);
            assertThat(leida.getSystemUser().code()).isEqualTo("op-admin");
            assertThat(leida.getSystemPermission().id()).isEqualTo(permisoId);
            assertThat(leida.getSystemPermission().code()).isEqualTo("reports.manage");
            assertThat(leida.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("un id inexistente no es un error, es vacio")
        void un_id_inexistente_es_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("listado")
    class Listado {

        @Test
        @DisplayName("findAll trae todas las asignaciones habilitadas")
        void find_all_trae_todas_las_asignaciones_habilitadas() {
            repository.save(asignacion(usuarioId, permisoId));
            repository.save(asignacion(usuarioId, otroPermisoId));

            List<SystemUserPermission> todas = repository.findAll();

            assertThat(todas).hasSize(2).extracting(sup -> sup.getSystemPermission().id())
                    .containsExactlyInAnyOrder(permisoId, otroPermisoId);
        }
    }

    @Nested
    @DisplayName("borrado logico")
    class BorradoLogico {

        @Test
        @DisplayName("delete no elimina la fila: la deshabilita, y deja de aparecer")
        void delete_deshabilita_en_vez_de_eliminar() {
            SystemUserPermission guardada = repository.save(asignacion(usuarioId, permisoId));

            repository.delete(guardada.getId());

            assertThat(repository.findById(guardada.getId())).isEmpty();
            // La fila sigue en la tabla: el SELECT nativo salta el filtro de Hibernate.
            Number habilitada = (Number) entityManager
                    .createNativeQuery("SELECT enabled FROM system_user_permissions WHERE id = :id")
                    .setParameter("id", guardada.getId()).getSingleResult();
            assertThat(habilitada.intValue()).isZero();
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate vuelve a habilitar una asignacion deshabilitada")
        void reactivate_vuelve_a_habilitar() {
            SystemUserPermission guardada = repository.save(asignacion(usuarioId, permisoId));
            repository.delete(guardada.getId());

            int filas = repository.reactivate(guardada.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivar un id inexistente no afecta filas")
        void reactivar_un_id_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }

    @Nested
    @DisplayName("busqueda de asignacion deshabilitada duplicada")
    class BusquedaDeshabilitada {

        @Test
        @DisplayName("encuentra el id de la fila deshabilitada para el mismo par usuario-permiso")
        void encuentra_el_id_de_la_fila_deshabilitada() {
            SystemUserPermission guardada = repository.save(asignacion(usuarioId, permisoId));
            repository.delete(guardada.getId());

            Optional<Long> disabledId = repository
                    .findDisabledIdBySystemUserAndSystemPermission(usuarioId, permisoId);

            assertThat(disabledId).contains(guardada.getId());
        }

        @Test
        @DisplayName("una asignacion habilitada no cuenta como deshabilitada duplicada")
        void una_asignacion_habilitada_no_cuenta() {
            repository.save(asignacion(usuarioId, permisoId));

            assertThat(
                    repository.findDisabledIdBySystemUserAndSystemPermission(usuarioId, permisoId))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("unicidad del par usuario-permiso")
    class Unicidad {

        @Test
        @DisplayName("el mismo par usuario-permiso no admite una segunda fila habilitada")
        void el_mismo_par_no_admite_una_segunda_fila() {
            repository.save(asignacion(usuarioId, permisoId));

            assertThatThrownBy(() -> repository.save(asignacion(usuarioId, permisoId)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
