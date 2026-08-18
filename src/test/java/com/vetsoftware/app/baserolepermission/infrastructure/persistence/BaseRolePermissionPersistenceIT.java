package com.vetsoftware.app.baserolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import com.vetsoftware.app.baserolepermission.domain.BaseRolePermission;
import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
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

/**
 * Rodaja de persistencia del adaptador de vinculos rol-permiso contra MySQL
 * real.
 *
 * <p>
 * Sin esta rodaja el {@code @EntityGraph(attributePaths = {"baseRole",
 * "basePermission"})} de {@code BaseRolePermissionJpaRepository}, el
 * {@code UPDATE base_role_permissions SET enabled = true} nativo de
 * {@code reactivate} y la consulta nativa de
 * {@code findDisabledIdByBaseRoleAndBasePermission} solo los ejercitaria
 * produccion. Ni el modulo, ni el submodulo, ni el permiso base, ni el rol base
 * los siembra {@code SchemaSeed}, asi que este test los inserta por su cuenta.
 */
@Import({JpaBaseRolePermissionRepository.class, BaseRolePermissionJpaMapper.class})
@DisplayName("JpaBaseRolePermissionRepository — persistencia de vinculos rol-permiso contra MySQL real")
class BaseRolePermissionPersistenceIT extends AbstractDataJpaTest {

    private static final Long MODULE_ID = 910L;
    private static final Long SUB_MODULE_ID = 911L;
    private static final Long BASE_PERMISSION_ID = 912L;
    private static final Long BASE_ROLE_ID = 913L;

    @Autowired
    private JpaBaseRolePermissionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarElCatalogo() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO modules (id, name, code, created_date, enabled)
                VALUES (:id, 'Clinica', 'CLIN-IT', '2026-01-01 00:00:00', true)
                """).setParameter("id", MODULE_ID).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO sub_modules (id, name, code, module_id, created_date, enabled)
                VALUES (:id, 'Consultas', 'CONS-IT', :moduleId, '2026-01-01 00:00:00', true)
                """).setParameter("id", SUB_MODULE_ID).setParameter("moduleId", MODULE_ID)
                .executeUpdate();
        entityManager
                .createNativeQuery(
                        """
                                INSERT IGNORE INTO base_permissions (id, name, code, sub_module_id, created_date, enabled)
                                VALUES (:id, 'Crear consulta', 'CONSULTA_CREATE-IT', :subModuleId, '2026-01-01 00:00:00', true)
                                """)
                .setParameter("id", BASE_PERMISSION_ID).setParameter("subModuleId", SUB_MODULE_ID)
                .executeUpdate();
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO base_roles (id, name, code, mandatory, created_date, enabled)
                VALUES (:id, 'Veterinario', 'VET-IT', false, '2026-01-01 00:00:00', true)
                """).setParameter("id", BASE_ROLE_ID).executeUpdate();
        entityManager.flush();
    }

    private BaseRolePermission nuevoVinculo() {
        return repository.save(BaseRolePermission.create(
                new BaseRoleRef(BASE_ROLE_ID, "Veterinario", "VET-IT"),
                new BasePermissionRef(BASE_PERMISSION_ID, "Crear consulta", "CONSULTA_CREATE-IT")));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva rol, permiso y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            BaseRolePermission guardado = nuevoVinculo();

            BaseRolePermission leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getBaseRole())
                    .isEqualTo(new BaseRoleRef(BASE_ROLE_ID, "Veterinario", "VET-IT"));
            assertThat(leido.getBasePermission()).isEqualTo(new BasePermissionRef(
                    BASE_PERMISSION_ID, "Crear consulta", "CONSULTA_CREATE-IT"));
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de un vinculo inexistente devuelve vacio")
        void find_by_id_de_un_vinculo_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listados {

        @Test
        @DisplayName("findAll trae los vinculos guardados")
        void find_all_trae_los_vinculos_guardados() {
            BaseRolePermission guardado = nuevoVinculo();

            List<BaseRolePermission> todos = repository.findAll();

            assertThat(todos).extracting(BaseRolePermission::getId).contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            BaseRolePermission guardado = nuevoVinculo();

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar un vinculo dado de baja")
        void reactivate_vuelve_a_habilitar_un_vinculo_dado_de_baja() {
            BaseRolePermission guardado = nuevoVinculo();
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(BaseRolePermission::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate sobre un vinculo inexistente no afecta filas")
        void reactivate_sobre_un_vinculo_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }

    @Nested
    @DisplayName("findDisabledIdByBaseRoleAndBasePermission")
    class BusquedaDeDeshabilitados {

        @Test
        @DisplayName("encuentra el id de un vinculo deshabilitado para ese mismo rol y permiso")
        void encuentra_el_id_de_un_vinculo_deshabilitado() {
            BaseRolePermission guardado = nuevoVinculo();
            repository.delete(guardado.getId());

            Optional<Long> encontrado = repository
                    .findDisabledIdByBaseRoleAndBasePermission(BASE_ROLE_ID, BASE_PERMISSION_ID);

            assertThat(encontrado).contains(guardado.getId());
        }

        @Test
        @DisplayName("un vinculo habilitado no cuenta como deshabilitado")
        void un_vinculo_habilitado_no_cuenta_como_deshabilitado() {
            nuevoVinculo();

            Optional<Long> encontrado = repository
                    .findDisabledIdByBaseRoleAndBasePermission(BASE_ROLE_ID, BASE_PERMISSION_ID);

            assertThat(encontrado).isEmpty();
        }

        @Test
        @DisplayName("sin vinculo entre ese rol y permiso devuelve vacio")
        void sin_vinculo_entre_rol_y_permiso_devuelve_vacio() {
            Optional<Long> encontrado = repository
                    .findDisabledIdByBaseRoleAndBasePermission(BASE_ROLE_ID, BASE_PERMISSION_ID);

            assertThat(encontrado).isEmpty();
        }
    }
}
