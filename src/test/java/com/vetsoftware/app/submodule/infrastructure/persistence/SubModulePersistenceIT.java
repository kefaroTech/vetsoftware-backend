package com.vetsoftware.app.submodule.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.submodule.domain.ModuleRef;
import com.vetsoftware.app.submodule.domain.SubModule;
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
 * Rodaja de persistencia del adaptador de submodulos contra MySQL real.
 *
 * <p>
 * Sin esta rodaja el {@code @EntityGraph(attributePaths = "module")} de
 * {@code SubModuleJpaRepository} y el {@code UPDATE sub_modules SET enabled =
 * true} nativo de {@code reactivate} solo los ejercitaria produccion. El modulo
 * no lo siembra {@code SchemaSeed} (es un catalogo maestro sin fila raiz comun
 * a otras features), asi que este test lo inserta por su cuenta.
 */
@Import({JpaSubModuleRepository.class, SubModuleJpaMapper.class})
@DisplayName("JpaSubModuleRepository — persistencia de submodulos contra MySQL real")
class SubModulePersistenceIT extends AbstractDataJpaTest {

    private static final Long FACTURACION_ID = 900L;
    private static final Long INVENTARIO_ID = 901L;

    @Autowired
    private JpaSubModuleRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLosModulos() {
        sembrarModulo(FACTURACION_ID, "Facturacion", "FACT-IT");
        sembrarModulo(INVENTARIO_ID, "Inventario", "INV-IT");
        entityManager.flush();
    }

    private void sembrarModulo(Long id, String nombre, String codigo) {
        entityManager.createNativeQuery("""
                INSERT IGNORE INTO modules (id, name, code, created_date, enabled)
                VALUES (:id, :nombre, :codigo, '2026-01-01 00:00:00', true)
                """).setParameter("id", id).setParameter("nombre", nombre)
                .setParameter("codigo", codigo).executeUpdate();
    }

    private SubModule nuevoSubModulo(String nombre, String codigo, Long moduleId, String moduleName,
            String moduleCode) {
        return repository.save(
                SubModule.create(nombre, codigo, new ModuleRef(moduleId, moduleName, moduleCode)));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva nombre, codigo, modulo y habilitacion")
        void guardar_y_releer_conserva_cada_campo() {
            SubModule guardado = nuevoSubModulo("Reportes", "REP-IT", FACTURACION_ID, "Facturacion",
                    "FACT-IT");

            SubModule leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getName()).isEqualTo("Reportes");
            assertThat(leido.getCode()).isEqualTo("REP-IT");
            assertThat(leido.getModule())
                    .isEqualTo(new ModuleRef(FACTURACION_ID, "Facturacion", "FACT-IT"));
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }

        @Test
        @DisplayName("findById de un submodulo inexistente devuelve vacio")
        void find_by_id_de_un_submodulo_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999999L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAll")
    class Listados {

        @Test
        @DisplayName("findAll trae los submodulos de todos los modulos")
        void find_all_trae_los_submodulos_de_todos_los_modulos() {
            nuevoSubModulo("Reportes", "REP-IT", FACTURACION_ID, "Facturacion", "FACT-IT");
            nuevoSubModulo("Kardex", "KDX-IT", INVENTARIO_ID, "Inventario", "INV-IT");

            List<SubModule> todos = repository.findAll();

            assertThat(todos).extracting(SubModule::getName).contains("Reportes", "Kardex");
        }
    }

    @Nested
    @DisplayName("delete y reactivate")
    class BajaYReactivacion {

        @Test
        @DisplayName("delete es una baja logica: no vuelve a aparecer en findById")
        void delete_es_una_baja_logica() {
            SubModule guardado = nuevoSubModulo("Reportes", "REP-IT", FACTURACION_ID, "Facturacion",
                    "FACT-IT");

            repository.delete(guardado.getId());

            assertThat(repository.findById(guardado.getId())).isEmpty();
        }

        @Test
        @DisplayName("reactivate vuelve a habilitar un submodulo dado de baja")
        void reactivate_vuelve_a_habilitar_un_submodulo_dado_de_baja() {
            SubModule guardado = nuevoSubModulo("Reportes", "REP-IT", FACTURACION_ID, "Facturacion",
                    "FACT-IT");
            repository.delete(guardado.getId());

            int filas = repository.reactivate(guardado.getId());

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(SubModule::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate sobre un submodulo inexistente no afecta filas")
        void reactivate_sobre_un_submodulo_inexistente_no_afecta_filas() {
            assertThat(repository.reactivate(999999L)).isZero();
        }
    }
}
