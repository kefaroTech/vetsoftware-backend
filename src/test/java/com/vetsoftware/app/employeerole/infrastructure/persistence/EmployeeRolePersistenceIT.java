package com.vetsoftware.app.employeerole.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.employeerole.domain.EmployeeRef;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.domain.RoleRef;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de persistencia de la asignacion empleado-rol contra MySQL real.
 *
 * <p>
 * El soft delete de {@code delete(Long)} lo hace Hibernate al interceptar el
 * {@code remove()} de la entidad ({@code @SQLDelete} + {@code @SQLRestriction}
 * en {@code EmployeeRoleJpaEntity}); un doble en memoria de
 * {@code EmployeeRoleRepository} no distinguiria eso de un borrado fisico, y lo
 * mismo aplica a {@code findDisabledIdByEmployeeAndRole} y {@code reactivate},
 * que son consultas/UPDATE nativos que solo el motor real ejercita de verdad.
 */
@Import({JpaEmployeeRoleRepository.class, EmployeeRoleJpaMapper.class})
@DisplayName("JpaEmployeeRoleRepository — CRUD y soft delete de la asignacion contra MySQL real")
class EmployeeRolePersistenceIT extends AbstractDataJpaTest {

    private static final Long ROLE_ID = 970L;

    private static final EmployeeRef EMPLEADO = new EmployeeRef(SchemaSeed.EMPLOYEE_ID, "EMP-001",
            "Ana Ruiz");
    private static final RoleRef ROL = new RoleRef(ROLE_ID, "Veterinario", "VET");

    @Autowired
    private JpaEmployeeRoleRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
        insert(("INSERT IGNORE INTO roles (id, name, code, company_id, created_date, enabled) "
                + "VALUES (:id, 'Veterinario', 'VET', %d, '2026-01-15 10:30:00', true)")
                .formatted(SchemaSeed.COMPANY_ID), ROLE_ID);
        entityManager.flush();
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
                .createNativeQuery("SELECT COUNT(*) FROM employee_roles WHERE id = :id")
                .setParameter("id", id).getSingleResult()).longValue();
    }

    /** Lee el flag real de la fila, tambien cuando esta deshabilitada. */
    private boolean estaHabilitada(Long id) {
        return ((Number) entityManager
                .createNativeQuery("SELECT enabled FROM employee_roles WHERE id = :id")
                .setParameter("id", id).getSingleResult()).intValue() == 1;
    }

    private EmployeeRole guardar() {
        return repository.save(EmployeeRole.create(EMPLEADO, ROL));
    }

    @Nested
    @DisplayName("ida y vuelta del agregado")
    class IdaYVuelta {

        @Test
        @DisplayName("guardar y releer conserva el empleado, el rol y el estado")
        void guardar_y_releer_conserva_cada_campo() {
            EmployeeRole guardado = guardar();

            EmployeeRole leido = repository.findById(guardado.getId()).orElseThrow();

            assertThat(leido.getEmployee()).isEqualTo(EMPLEADO);
            assertThat(leido.getRole()).isEqualTo(ROL);
            assertThat(leido.isEnabled()).isTrue();
            assertThat(leido.getCreatedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("lecturas")
    class Lectura {

        @Test
        @DisplayName("findById de una asignacion inexistente devuelve vacio")
        void find_by_id_inexistente_devuelve_vacio() {
            assertThat(repository.findById(999_999L)).isEmpty();
        }

        @Test
        @DisplayName("findAll trae la asignacion guardada")
        void find_all_trae_la_asignacion_guardada() {
            EmployeeRole guardado = guardar();

            assertThat(repository.findAll()).extracting(EmployeeRole::getId)
                    .contains(guardado.getId());
        }
    }

    @Nested
    @DisplayName("bajas")
    class Bajas {

        @Test
        @DisplayName("delete(id) es un soft delete: la fila sigue en la tabla, solo deshabilitada")
        void delete_es_soft_delete() {
            EmployeeRole guardado = guardar();

            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).isEmpty();
            assertThat(contarFilasFisicas(guardado.getId())).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("reactivacion")
    class Reactivacion {

        @Test
        @DisplayName("reactivate(id) devuelve al listado activo la fila soft-deleteada")
        void reactivate_devuelve_al_listado_activo() {
            EmployeeRole guardado = guardar();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId());
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(repository.findById(guardado.getId())).map(EmployeeRole::isEnabled)
                    .contains(true);
        }

        @Test
        @DisplayName("reactivate de un id inexistente no afecta ninguna fila")
        void reactivate_de_un_id_inexistente_devuelve_cero() {
            assertThat(repository.reactivate(999_999L)).isZero();
        }
    }

    /**
     * El aislamiento de esta feature no es una columna sino un EXISTS sobre
     * {@code employees}: la asignacion no tiene {@code company_id} propio. Eso solo
     * lo ejercita el motor real — un doble en memoria del repositorio no
     * distinguiria un WHERE con la subconsulta de uno sin ella.
     */
    @Nested
    @DisplayName("aislamiento entre empresas")
    class Tenancy {

        @Test
        @DisplayName("findByIdAndCompanyId encuentra la asignacion por la empresa del empleado")
        void find_by_id_and_company_id_encuentra_la_asignacion() {
            EmployeeRole guardado = guardar();
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
        }

        @Test
        @DisplayName("findByIdAndCompanyId no ve la asignacion desde otra empresa")
        void find_by_id_and_company_id_no_ve_la_de_otra_empresa() {
            EmployeeRole guardado = guardar();
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.findByIdAndCompanyId(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("reactivate acotado a su empresa devuelve al listado activo la fila")
        void reactivate_acotado_a_su_empresa_reactiva() {
            EmployeeRole guardado = guardar();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), SchemaSeed.COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isEqualTo(1);
            assertThat(estaHabilitada(guardado.getId())).isTrue();
        }

        /**
         * La fuga que cierra este test: sin el EXISTS, este UPDATE afectaba la fila y
         * el empleado de la otra empresa recuperaba un privilegio revocado.
         */
        @Test
        @DisplayName("reactivate con el companyId de otra empresa afecta 0 filas y la deja deshabilitada")
        void reactivate_desde_otra_empresa_no_afecta_ninguna_fila() {
            EmployeeRole guardado = guardar();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            int filas = repository.reactivate(guardado.getId(), SchemaSeed.OTRA_COMPANY_ID);
            entityManager.clear();

            assertThat(filas).isZero();
            assertThat(estaHabilitada(guardado.getId())).isFalse();
            assertThat(repository.findById(guardado.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("busqueda de asignaciones desactivadas — lo que usa la reactivacion implicita al crear")
    class Desactivadas {

        @Test
        @DisplayName("findDisabledIdByEmployeeAndRole encuentra el id de la fila soft-deleteada")
        void find_disabled_id_encuentra_la_fila() {
            EmployeeRole guardado = guardar();
            repository.delete(guardado.getId());
            entityManager.flush();
            entityManager.clear();

            Optional<Long> id = repository.findDisabledIdByEmployeeAndRole(SchemaSeed.EMPLOYEE_ID,
                    ROLE_ID);

            assertThat(id).contains(guardado.getId());
        }

        @Test
        @DisplayName("findDisabledIdByEmployeeAndRole no ve una fila que sigue activa")
        void find_disabled_id_no_ve_una_fila_activa() {
            guardar();

            assertThat(repository.findDisabledIdByEmployeeAndRole(SchemaSeed.EMPLOYEE_ID, ROLE_ID))
                    .isEmpty();
        }
    }
}
