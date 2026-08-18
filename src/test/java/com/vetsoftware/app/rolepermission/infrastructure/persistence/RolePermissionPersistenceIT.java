package com.vetsoftware.app.rolepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.rolepermission.application.port.out.RolePermissionRepository.DisabledRolePermissionLookup;
import com.vetsoftware.app.rolepermission.domain.PermissionRef;
import com.vetsoftware.app.rolepermission.domain.RolePermission;
import com.vetsoftware.app.rolepermission.domain.RoleRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
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
 * Rodaja de persistencia de la asignacion rol-permiso contra MySQL real.
 *
 * <p>
 * <b>Por que un doble no sirve aqui.</b> El soft delete de {@code delete(Long)}
 * lo hace Hibernate al interceptar el {@code remove()} de una entidad cargada
 * ({@code @SQLDelete} + {@code @SQLRestriction}); un stub en memoria no
 * distinguiria eso de un borrado fisico. Y la baja en bloque
 * ({@code deleteAllByIds}) tiene que dejar la fila en el mismo sitio que la
 * individual: solo pasando por el motor real se ve que ambas la deshabilitan en
 * vez de borrarla (ver {@code Bajas}), que es lo que permite reactivarla
 * despues conservando su id.
 */
@Import({JpaRolePermissionRepository.class, RolePermissionJpaMapper.class})
@DisplayName("JpaRolePermissionRepository — CRUD, tenencia por rol y soft delete contra MySQL real")
class RolePermissionPersistenceIT extends AbstractDataJpaTest {

    private static final Long MODULE_ID = 960L;
    private static final Long SUB_MODULE_ID = 961L;
    private static final Long ROLE_ID = 962L;
    private static final Long ROLE_OTRA_ID = 963L;
    private static final Long PERMISSION_ID = 964L;
    private static final Long PERMISSION_2_ID = 965L;

    private static final RoleRef ROL = new RoleRef(ROLE_ID, "Veterinario", "VET");
    private static final RoleRef ROL_AJENO = new RoleRef(ROLE_OTRA_ID, "Recepcion ajena",
            "REC-AJENA");
    private static final PermissionRef PERMISO = new PermissionRef(PERMISSION_ID, "Ver animales",
            "ANIMAL_READ");
    private static final PermissionRef PERMISO_2 = new PermissionRef(PERMISSION_2_ID,
            "Crear animales", "ANIMAL_CREATE");

    @Autowired
    private JpaRolePermissionRepository repository;

    /**
     * La baja en cascada del rol no pasa por el puerto de salida de esta feature
     * —la consume {@code role} via {@code RolePermissionChildrenCascadePort}—, asi
     * que se ejercita sobre el repositorio de Spring Data directamente.
     */
    @Autowired
    private RolePermissionJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        insert("INSERT IGNORE INTO modules (id, name, code, created_date, enabled) "
                + "VALUES (:id, 'Modulo test', 'MOD-TEST', '2026-01-01 00:00:00', true)",
                MODULE_ID);
        insert(("INSERT IGNORE INTO sub_modules (id, name, code, module_id, created_date, enabled) "
                + "VALUES (:id, 'Submodulo test', 'SUBMOD-TEST', %d, '2026-01-01 00:00:00', true)")
                .formatted(MODULE_ID), SUB_MODULE_ID);
        rol(ROLE_ID, "Veterinario", "VET", SchemaSeed.COMPANY_ID);
        rol(ROLE_OTRA_ID, "Recepcion ajena", "REC-AJENA", SchemaSeed.OTRA_COMPANY_ID);
        permiso(PERMISSION_ID, "Ver animales", "ANIMAL_READ");
        permiso(PERMISSION_2_ID, "Crear animales", "ANIMAL_CREATE");
        entityManager.flush();
    }

    private void rol(Long id, String name, String code, Long companyId) {
        insert(("INSERT IGNORE INTO roles (id, name, code, company_id, created_date, enabled) "
                + "VALUES (:id, '%s', '%s', %d, '2026-01-15 10:30:00', true)")
                .formatted(name, code, companyId), id);
    }

    private void permiso(Long id, String name, String code) {
        insert(("INSERT IGNORE INTO permissions "
                + "(id, name, code, company_id, sub_module_id, created_date, enabled) "
                + "VALUES (:id, '%s', '%s', %d, %d, '2026-01-15 10:30:00', true)")
                .formatted(name, code, SchemaSeed.COMPANY_ID, SUB_MODULE_ID), id);
    }

    private void insert(String sql, Long id) {
        entityManager.createNativeQuery(sql).setParameter("id", id).executeUpdate();
    }

    /**
     * Cuenta filas fisicas sin pasar por Hibernate, para esquivar
     * el @SQLRestriction.
     */
    private long contarFilasFisicas(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT COUNT(*) FROM role_permissions WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    private RolePermission guardar(RoleRef role, PermissionRef permission) {
        return repository.save(RolePermission.create(role, permission));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva el rol, el permiso y el estado")
        void guardar_y_releer_conserva_cada_campo() {
            RolePermission guardado = guardar(ROL, PERMISO);

            RolePermission leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getRole()).isEqualTo(ROL);
            assertThat(leido.getPermission()).isEqualTo(PERMISO);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("saveAll persiste el lote y conserva el orden de vuelta")
        void save_all_persiste_el_lote() {
            List<RolePermission> guardados = repository.saveAll(List.of(
                    RolePermission.create(ROL, PERMISO), RolePermission.create(ROL, PERMISO_2)));

            assertThat(guardados).extracting(RolePermission::getPermission).containsExactly(PERMISO,
                    PERMISO_2);
            assertThat(repository.findAllByRoleId(ROLE_ID)).hasSize(2);
        }

        @Test
        @DisplayName("findAll trae las asignaciones de todas las empresas")
        void find_all_trae_todas_las_empresas() {
            guardar(ROL, PERMISO);
            guardar(ROL_AJENO, PERMISO_2);

            assertThat(repository.findAll()).hasSizeGreaterThanOrEqualTo(2);
        }
    }

    @Nested
    @DisplayName("listados por rol")
    class ListadosPorRol {

        @Test
        @DisplayName("findAllByRoleId no trae las asignaciones de otro rol")
        void find_all_by_role_id_no_mezcla_roles() {
            RolePermission propia = guardar(ROL, PERMISO);
            guardar(ROL_AJENO, PERMISO_2);

            assertThat(repository.findAllByRoleId(ROLE_ID)).extracting(RolePermission::getId)
                    .containsExactly(propia.getId());
        }

        @Test
        @DisplayName("findAllByRoleCompanyId agrega por la empresa del rol, no por una FK propia")
        void find_all_by_role_company_id_filtra_por_la_empresa_del_rol() {
            RolePermission propia = guardar(ROL, PERMISO);
            guardar(ROL_AJENO, PERMISO_2);

            assertThat(repository.findAllByRoleCompanyId(SchemaSeed.COMPANY_ID))
                    .extracting(RolePermission::getId).containsExactly(propia.getId());
        }
    }

    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenencia {

        @Test
        @DisplayName("findByIdAndCompanyId no devuelve la asignacion de un rol de otra empresa")
        void no_devuelve_la_asignacion_de_otra_empresa() {
            RolePermission ajena = guardar(ROL_AJENO, PERMISO);

            assertThat(repository.findByIdAndCompanyId(ajena.getId(), SchemaSeed.COMPANY_ID))
                    .isEmpty();
            assertThat(repository.findByIdAndCompanyId(ajena.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .map(RolePermission::getId).contains(ajena.getId());
        }
    }

    @Nested
    @DisplayName("bajas")
    class Bajas {

        @Test
        @DisplayName("delete(id) es un soft delete: la fila sigue en la tabla, solo deshabilitada")
        void delete_es_soft_delete() {
            RolePermission guardada = guardar(ROL, PERMISO);

            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();
            assertThat(contarFilasFisicas(guardada.getId())).isEqualTo(1L);
        }

        /**
         * Las dos vias de baja tienen que dejar la fila en el mismo sitio. Hasta la
         * campaña de cobertura no era asi: deleteAllByIds usaba deleteAllByIdInBatch,
         * un DELETE en bloque que Hibernate NO pasa por el @SQLDelete de la entidad, y
         * borraba fisicamente lo que delete(id) solo deshabilita.
         * SyncRolePermissionsService la usa para las bajas del reconciliador, asi que
         * un permiso quitado y vuelto a asignar despues perdia su id original en vez de
         * reactivar la fila — justo lo que ese servicio intenta hacer con
         * findDisabledByRoleAndPermissions/reactivateAllByIds. Ahora emite un UPDATE
         * ... SET enabled = false en bloque.
         */
        @Test
        @DisplayName("deleteAllByIds tambien es soft delete: la fila queda, solo deshabilitada")
        void delete_all_by_ids_es_soft_delete_igual_que_delete() {
            RolePermission guardada = guardar(ROL, PERMISO);

            repository.deleteAllByIds(List.of(guardada.getId()));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardada.getId())).isEmpty();
            assertThat(contarFilasFisicas(guardada.getId())).isEqualTo(1L);
        }

        /**
         * El motivo por el que la baja en bloque tiene que ser soft: la fila
         * deshabilitada es la que despues encuentra el reconciliador para reactivar el
         * permiso conservando su id. Con el borrado fisico anterior esta busqueda no
         * devolvia nada y el permiso se recreaba con id nuevo.
         */
        @Test
        @DisplayName("tras deleteAllByIds la fila sigue siendo reactivable con su id original")
        void tras_delete_all_by_ids_la_fila_sigue_siendo_reactivable() {
            RolePermission guardada = guardar(ROL, PERMISO);

            repository.deleteAllByIds(List.of(guardada.getId()));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findDisabledIdByRoleAndPermission(ROLE_ID, PERMISSION_ID))
                    .contains(guardada.getId());
            assertThat(repository.reactivateAllByIds(List.of(guardada.getId()))).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).map(RolePermission::getId)
                    .contains(guardada.getId());
        }

        @Test
        @DisplayName("deleteAllByIds con lista vacia no ejecuta ningun DELETE")
        void delete_all_by_ids_con_lista_vacia_no_hace_nada() {
            RolePermission guardada = guardar(ROL, PERMISO);

            repository.deleteAllByIds(List.of());

            assertThat(contarFilasFisicas(guardada.getId())).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate(id) devuelve al listado activo la fila soft-deleteada")
        void reactivate_devuelve_al_listado_activo() {
            RolePermission guardada = guardar(ROL, PERMISO);
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardada.getId());
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).map(RolePermission::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate(id, companyId) no revive la fila si la empresa no coincide con la del rol")
        void reactivate_con_empresa_no_coincidente_no_toca_nada() {
            RolePermission guardada = guardar(ROL, PERMISO);
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID);

            assertThat(filas).isZero();
        }

        @Test
        @DisplayName("reactivate(id, companyId) revive la fila cuando la empresa es la del rol")
        void reactivate_con_la_empresa_correcta_revive_la_fila() {
            RolePermission guardada = guardar(ROL, PERMISO);
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardada.getId(), SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardada.getId())).isPresent();
        }

        @Test
        @DisplayName("reactivateAllByIds revive varias filas de una vez")
        void reactivate_all_by_ids_revive_varias_filas() {
            RolePermission uno = guardar(ROL, PERMISO);
            RolePermission dos = guardar(ROL, PERMISO_2);
            repository.delete(uno.getId());
            repository.delete(dos.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivateAllByIds(List.of(uno.getId(), dos.getId()));
            entityManager.clear();

            assertThat(filas).isEqualTo(2);
            assertThat(repository.findAllByRoleId(ROLE_ID)).hasSize(2);
        }

        @Test
        @DisplayName("reactivateAllByIds con coleccion vacia no ejecuta ningun UPDATE y devuelve 0")
        void reactivate_all_by_ids_con_coleccion_vacia_devuelve_cero() {
            assertThat(repository.reactivateAllByIds(List.of())).isZero();
        }
    }

    @Nested
    @DisplayName("busqueda de filas desactivadas — lo que usa el reconciliador de SyncRolePermissionsService")
    class Desactivadas {

        @Test
        @DisplayName("findDisabledIdByRoleAndPermission encuentra el id de la fila soft-deleteada")
        void find_disabled_id_encuentra_la_fila() {
            RolePermission guardada = guardar(ROL, PERMISO);
            repository.delete(guardada.getId());
            entityManager.flush();
            entityManager.clear();

            Optional<Long> id = repository.findDisabledIdByRoleAndPermission(ROLE_ID,
                    PERMISSION_ID);

            assertThat(id).contains(guardada.getId());
        }

        @Test
        @DisplayName("findDisabledIdByRoleAndPermission no ve una fila que sigue activa")
        void find_disabled_id_no_ve_una_fila_activa() {
            guardar(ROL, PERMISO);

            assertThat(repository.findDisabledIdByRoleAndPermission(ROLE_ID, PERMISSION_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("findDisabledByRoleAndPermissions trae el lote de filas desactivadas del rol")
        void find_disabled_by_role_and_permissions_trae_el_lote() {
            RolePermission uno = guardar(ROL, PERMISO);
            RolePermission dos = guardar(ROL, PERMISO_2);
            repository.delete(uno.getId());
            repository.delete(dos.getId());
            entityManager.flush();
            entityManager.clear();

            List<DisabledRolePermissionLookup> desactivadas = repository
                    .findDisabledByRoleAndPermissions(ROLE_ID,
                            List.of(PERMISSION_ID, PERMISSION_2_ID));

            assertThat(desactivadas).extracting(DisabledRolePermissionLookup::permissionId)
                    .containsExactlyInAnyOrder(PERMISSION_ID, PERMISSION_2_ID);
        }

        @Test
        @DisplayName("findDisabledByRoleAndPermissions con coleccion vacia no consulta y devuelve vacio")
        void find_disabled_by_role_and_permissions_con_coleccion_vacia_devuelve_vacio() {
            assertThat(repository.findDisabledByRoleAndPermissions(ROLE_ID, List.of())).isEmpty();
        }
    }

    /**
     * Las mutaciones en bloque acotadas por empresa. Se prueban contra MySQL real
     * porque lo que hay que ver es el {@code JOIN roles} del UPDATE: la empresa no
     * cuelga de {@code role_permissions}, vive en la tabla padre, asi que el filtro
     * viaja por la asociacion y un doble en memoria no diria nada del SQL emitido.
     *
     * <p>
     * En estas operaciones no hay lectura previa que valide la propiedad —el
     * servicio decide cuantas filas toco mirando el entero devuelto—, de modo que
     * el {@code WHERE} es toda la barrera. El caso que importa es el de la fila que
     * <em>existe</em> y es de otro tenant: tiene que devolver 0 y dejarla intacta.
     */
    @Nested
    @DisplayName("mutaciones en bloque acotadas por empresa")
    class BloqueAcotadoPorEmpresa {

        private boolean estaActiva(Long id) {
            return ((Number) entityManager
                    .createNativeQuery("SELECT enabled FROM role_permissions WHERE id = :id")
                    .setParameter("id", id).getSingleResult()).intValue() == 1;
        }

        @Test
        @DisplayName("reactivateAllByIds acotado revive las filas de la empresa")
        void reactivate_all_by_ids_acotado_revive_las_propias() {
            RolePermission uno = guardar(ROL, PERMISO);
            RolePermission dos = guardar(ROL, PERMISO_2);
            repository.delete(uno.getId());
            repository.delete(dos.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivateAllByIds(List.of(uno.getId(), dos.getId()),
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isEqualTo(2);
            assertThat(repository.findAllByRoleId(ROLE_ID)).hasSize(2);
        }

        @Test
        @DisplayName("reactivateAllByIds no revive la asignacion de otra empresa: 0 filas y sigue desactivada")
        void reactivate_all_by_ids_no_revive_la_de_otra_empresa() {
            RolePermission ajena = guardar(ROL_AJENO, PERMISO);
            repository.delete(ajena.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivateAllByIds(List.of(ajena.getId()),
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isZero();
            assertThat(contarFilasFisicas(ajena.getId())).isEqualTo(1);
            assertThat(estaActiva(ajena.getId())).isFalse();
        }

        @Test
        @DisplayName("un id ajeno colado en el lote no se cuela: solo revive el propio")
        void un_id_ajeno_colado_en_el_lote_no_revive() {
            RolePermission propia = guardar(ROL, PERMISO);
            RolePermission ajena = guardar(ROL_AJENO, PERMISO_2);
            repository.delete(propia.getId());
            repository.delete(ajena.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivateAllByIds(List.of(propia.getId(), ajena.getId()),
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(estaActiva(propia.getId())).isTrue();
            assertThat(estaActiva(ajena.getId())).isFalse();
        }

        @Test
        @DisplayName("deleteAllByIds acotado deshabilita las propias y respeta las ajenas")
        void delete_all_by_ids_acotado_respeta_las_ajenas() {
            RolePermission propia = guardar(ROL, PERMISO);
            RolePermission ajena = guardar(ROL_AJENO, PERMISO_2);
            entityManager.flush();
            entityManager.clear();

            repository.deleteAllByIds(List.of(propia.getId(), ajena.getId()),
                    SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(estaActiva(propia.getId())).isFalse();
            assertThat(estaActiva(ajena.getId())).isTrue();
        }

        @Test
        @DisplayName("deleteAllByIds acotado con lista vacia no ejecuta ningun UPDATE")
        void delete_all_by_ids_acotado_con_lista_vacia() {
            RolePermission guardada = guardar(ROL, PERMISO);
            entityManager.flush();
            entityManager.clear();

            repository.deleteAllByIds(List.of(), SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(estaActiva(guardada.getId())).isTrue();
        }

        @Test
        @DisplayName("reactivateAllByIds acotado con coleccion vacia devuelve 0")
        void reactivate_all_by_ids_acotado_con_coleccion_vacia() {
            assertThat(repository.reactivateAllByIds(List.of(), SchemaSeed.COMPANY_ID)).isZero();
        }

        @Test
        @DisplayName("disableAllByRoleId deshabilita los permisos del rol propio")
        void disable_all_by_role_id_deshabilita_los_del_rol_propio() {
            RolePermission uno = guardar(ROL, PERMISO);
            RolePermission dos = guardar(ROL, PERMISO_2);
            entityManager.flush();
            entityManager.clear();

            int filas = jpaRepository.disableAllByRoleId(ROLE_ID, SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isEqualTo(2);
            assertThat(estaActiva(uno.getId())).isFalse();
            assertThat(estaActiva(dos.getId())).isFalse();
        }

        @Test
        @DisplayName("disableAllByRoleId no toca el rol de otra empresa aunque acierte el id")
        void disable_all_by_role_id_no_toca_el_rol_ajeno() {
            RolePermission ajena = guardar(ROL_AJENO, PERMISO);
            entityManager.flush();
            entityManager.clear();

            int filas = jpaRepository.disableAllByRoleId(ROLE_OTRA_ID, SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isZero();
            assertThat(estaActiva(ajena.getId())).isTrue();
        }
    }
}
