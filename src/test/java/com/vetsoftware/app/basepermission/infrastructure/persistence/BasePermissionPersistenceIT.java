package com.vetsoftware.app.basepermission.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.basepermission.domain.BasePermission;
import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaMapper;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia del adaptador de permisos base contra MySQL real.
 *
 * <p>
 * Sin esta rodaja el {@code @EntityGraph(attributePaths = "subModule")} de
 * {@code BasePermissionJpaRepository} y el {@code UPDATE base_permissions SET
 * enabled = true} nativo de {@code reactivate} solo los ejercitaria produccion.
 * Ni el modulo ni el submodulo los siembra {@code SchemaSeed} (son catalogos
 * maestros sin fila raiz comun a otras features), asi que este test los inserta
 * por su cuenta.
 */
@Import({JpaBasePermissionRepository.class, BasePermissionJpaMapper.class,
        SubModuleJpaMapper.class})
@DisplayName("JpaBasePermissionRepository — persistencia de permisos base contra MySQL real")
class BasePermissionPersistenceIT extends AbstractDataJpaTest {

    private static final Long MODULE_ID = 900L;
    private static final Long VENTAS_ID = 900L;
    private static final Long INVENTARIO_ID = 901L;

    @Autowired
    private JpaBasePermissionRepository repository;

    @Autowired
    private SubModuleJpaRepository subModuleJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarModuloYSubModulos() {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO modules (id, name, code, created_date)
                VALUES (:id, 'Facturacion', 'FACT-IT', '2026-01-01 00:00:00')
                """).setParameter("id", MODULE_ID).executeUpdate();
        sembrarSubModulo(VENTAS_ID, "Ventas", "VEN-IT");
        sembrarSubModulo(INVENTARIO_ID, "Inventario", "INV-IT");
        entityManager.flush();
    }

    private void sembrarSubModulo(Long id, String nombre, String codigo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO sub_modules (id, name, code, module_id, created_date)
                VALUES (:id, :nombre, :codigo, :moduleId, '2026-01-01 00:00:00')
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).setParameter("moduleId", MODULE_ID).executeUpdate();
    }

    private BasePermission nuevoBasePermission(String nombre, String codigo, Long subModuleId,
            String subModuleName, String subModuleCode) {
        return repository.save(BasePermission.create(nombre, codigo,
                new SubModuleRef(subModuleId, subModuleName, subModuleCode)));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva nombre, codigo, submodulo y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            BasePermission guardado = nuevoBasePermission("Crear factura", "INVOICE_CREATE-IT",
                    VENTAS_ID, "Ventas", "VEN-IT");

            BasePermission leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Crear factura");
            assertThat(leido.getCode()).isEqualTo("INVOICE_CREATE-IT");
            assertThat(leido.getSubModule())
                    .isEqualTo(new SubModuleRef(VENTAS_ID, "Ventas", "VEN-IT"));
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de un permiso base inexistente devuelve vacio")
        void find_by_id_de_un_permiso_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listados {

        @Test
        @DisplayName("findAll trae los permisos base de todos los submodulos")
        void find_all_trae_los_permisos_de_todos_los_submodulos() {
            nuevoBasePermission("Crear factura", "INVOICE_CREATE-IT", VENTAS_ID, "Ventas",
                    "VEN-IT");
            nuevoBasePermission("Ver kardex", "INVENTORY_VIEW-IT", INVENTARIO_ID, "Inventario",
                    "INV-IT");

            List<BasePermission> todos = repository.findAll();

            assertThat(todos).extracting(BasePermission::getName).contains("Crear factura",
                    "Ver kardex");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            BasePermission guardado = nuevoBasePermission("Crear factura", "INVOICE_CREATE-IT",
                    VENTAS_ID, "Ventas", "VEN-IT");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar un permiso base dado de baja")
        void reactivate_vuelve_a_habilitar_un_permiso_dado_de_baja() {
            BasePermission guardado = nuevoBasePermission("Crear factura", "INVOICE_CREATE-IT",
                    VENTAS_ID, "Ventas", "VEN-IT");
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(BasePermission::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate sobre un permiso base inexistente no afecta filas")
        void reactivate_sobre_un_permiso_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }
}
