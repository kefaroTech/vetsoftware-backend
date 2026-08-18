package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.SchemaSeed;
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
 * Rodaja de persistencia del set atomico de sedes de un empleado contra MySQL
 * real. El reemplazo completo (desactivar todo, luego reactivar/insertar el
 * objetivo) depende de como distingue el UPDATE nativo una fila reactivable de
 * una que hay que insertar: eso es justo lo que un doble de
 * {@code EmployeeBranchRepository} no puede exigir sin definir el mismo
 * contrato que dice verificar.
 */
@Import(JpaEmployeeBranchRepository.class)
@DisplayName("JpaEmployeeBranchRepository — set atomico de sedes contra MySQL real")
class EmployeeBranchPersistenceIT extends AbstractDataJpaTest {

    @Autowired
    private JpaEmployeeBranchRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void sembrarLasFilasRaiz() {
        SchemaSeed.seed(entityManager);
    }

    @Nested
    @DisplayName("replaceBranches")
    class ReemplazoAtomico {

        @Test
        @DisplayName("deja al empleado con exactamente las sedes pedidas")
        void deja_al_empleado_con_exactamente_las_sedes_pedidas() {
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID));

            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID))
                    .containsExactly(SchemaSeed.BRANCH_ID);
        }

        @Test
        @DisplayName("reasignar desactiva la sede que ya no esta y agrega la nueva")
        void reasignar_desactiva_la_que_sobra_y_agrega_la_nueva() {
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID));

            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.OTRA_BRANCH_ID));

            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID))
                    .containsExactly(SchemaSeed.OTRA_BRANCH_ID);
        }

        @Test
        @DisplayName("una sede quitada y vuelta a pedir se reactiva, no choca con el unique")
        void una_sede_quitada_y_vuelta_a_pedir_se_reactiva() {
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID, SchemaSeed.OTRA_BRANCH_ID));
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.OTRA_BRANCH_ID));

            // BRANCH_ID quedo desactivada por el paso anterior: si reactivate() no
            // funcionara, este insert chocaria con el unique (employee_id, branch_id).
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID, SchemaSeed.OTRA_BRANCH_ID));

            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID))
                    .containsExactlyInAnyOrder(SchemaSeed.BRANCH_ID, SchemaSeed.OTRA_BRANCH_ID);
        }

        @Test
        @DisplayName("un set vacio deja al empleado sin ninguna sede")
        void un_set_vacio_deja_al_empleado_sin_ninguna_sede() {
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID));

            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID, List.of());

            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID)).isEmpty();
        }

        @Test
        @DisplayName("no toca las sedes asignadas a otro empleado")
        void no_toca_las_sedes_de_otro_empleado() {
            repository.replaceBranches(SchemaSeed.OTRO_EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.OTRA_BRANCH_ID));

            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID));

            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.OTRO_EMPLOYEE_ID))
                    .containsExactly(SchemaSeed.OTRA_BRANCH_ID);
        }

        @Test
        @DisplayName("con la empresa de otro tenant no desactiva ni reactiva nada")
        void con_otra_empresa_no_escribe_nada() {
            // El empleado y las sedes son de COMPANY_ID. Pasando OTRA_COMPANY_ID, los dos
            // EXISTS del UPDATE fallan: ni el disableAll borra lo vigente ni el reactivate
            // resucita nada. Antes de acotar, este mismo par de ids reescribia las sedes
            // del empleado de otra empresa.
            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.COMPANY_ID,
                    List.of(SchemaSeed.BRANCH_ID));

            repository.replaceBranches(SchemaSeed.EMPLOYEE_ID, SchemaSeed.OTRA_COMPANY_ID,
                    List.of(SchemaSeed.OTRA_BRANCH_ID));

            // Ni se desactivo lo vigente ni se inserto la sede pedida: las tres vias
            // (disableAll, reactivate e insert) van acotadas por la misma empresa.
            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID))
                    .containsExactly(SchemaSeed.BRANCH_ID);
        }
    }

    @Nested
    @DisplayName("findBranchIdsByEmployeeId")
    class Lectura {

        @Test
        @DisplayName("un empleado sin asignaciones devuelve una lista vacia")
        void un_empleado_sin_asignaciones_devuelve_lista_vacia() {
            assertThat(repository.findBranchIdsByEmployeeId(SchemaSeed.EMPLOYEE_ID)).isEmpty();
        }
    }
}
