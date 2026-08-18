package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EmployeeBranchJpaRepository extends JpaRepository<EmployeeBranchJpaEntity, Long> {

    // Ids de las sedes asignadas (vigentes) a un empleado. El
    // @SQLRestriction("enabled = true") ya
    // excluye las
    // asignaciones desactivadas de ambas puntas de la relación via el filtro sobre
    // employee_branches.
    @Query("""
            select eb.branch.id from EmployeeBranchJpaEntity eb where eb.employee.id = :employeeId
            """)
    List<Long> findBranchIdsByEmployeeId(Long employeeId);

    // Asignaciones vigentes (con nombre de la sede) de un conjunto de empleados,
    // para pintar las
    // sedes en el
    // listado/detalle. El @SQLRestriction("enabled = true") de la entidad ya
    // excluye asignaciones
    // desactivadas.
    @Query("""
            select eb.employee.id as employeeId, eb.branch.id as branchId,
              eb.branch.name as branchName from EmployeeBranchJpaEntity eb where eb.employee.id in :employeeIds
            """)
    List<EmployeeBranchAssignmentView> findAssignmentsByEmployeeIds(
            @Param("employeeIds") List<Long> employeeIds);

    // Soft-delete de TODAS las asignaciones vigentes del empleado (primer paso del
    // set atómico).
    // Nativa para saltar
    // el @SQLRestriction y actualizar por employee_id directamente.
    //
    // El employee_id lo elige el cliente (viene en la URL del endpoint), así que
    // acotar por él NO es acotar por empresa: es una FK ajena, el mismo criterio de
    // BE-29. La empresa no cuelga de employee_branches sino del empleado, así que
    // el filtro viaja por un EXISTS contra employees — misma ruta que usa
    // EmployeeQueryPort.existsByIdAndCompanyId, que el servicio ya comprueba antes.
    // Aquí esa comprobación deja de ser la única barrera y pasa a estar en el SQL.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employee_branches eb
            SET eb.enabled = false
            WHERE eb.employee_id = :employeeId
              AND eb.enabled = true
              AND EXISTS (SELECT 1
                          FROM employees e
                          WHERE e.id = eb.employee_id
                            AND e.company_id = :companyId)
            """, nativeQuery = true)
    void disableAllByEmployeeId(@Param("employeeId") Long employeeId,
            @Param("companyId") Long companyId);

    // Reactiva una asignación previamente desactivada (evita insertar un duplicado
    // que violaría el
    // unique). Devuelve
    // el nº de filas afectadas: 0 → no existía esa pareja, hay que insertarla.
    //
    // Reactivación = no hay lectura previa de ESTA fila; el servicio decide si
    // existía mirando las filas afectadas, así que el WHERE es toda la seguridad.
    // Las dos puntas de la relación se acotan a la empresa por EXISTS: el empleado
    // (employees.company_id) y la sede (branches.company_id). Sin ellos, una pareja
    // (empleado, sede) de otro tenant se reactivaba con solo escribir sus ids.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            UPDATE employee_branches eb
            SET eb.enabled = true
            WHERE eb.employee_id = :employeeId
              AND eb.branch_id = :branchId
              AND eb.enabled = false
              AND EXISTS (SELECT 1
                          FROM employees e
                          WHERE e.id = eb.employee_id
                            AND e.company_id = :companyId)
              AND EXISTS (SELECT 1
                          FROM branches b
                          WHERE b.id = eb.branch_id
                            AND b.company_id = :companyId)
            """, nativeQuery = true)
    int reactivate(@Param("employeeId") Long employeeId, @Param("branchId") Long branchId,
            @Param("companyId") Long companyId);

    // Inserta una asignación nueva. Solo se llama cuando reactivate devolvió 0 (no
    // había fila), así
    // que no choca con
    // el unique (employee_id, branch_id).
    //
    // INSERT … SELECT y no VALUES: el reactivate acotado devuelve 0 tanto si la
    // pareja no existia como si es de OTRA empresa, y con un VALUES ciego el
    // segundo caso terminaba insertando la asignacion ajena que el UPDATE acababa
    // de rechazar. El SELECT no produce ninguna fila si el empleado o la sede no
    // son de :companyId, asi que las dos vias quedan acotadas igual.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO employee_branches (employee_id, branch_id, created_date, enabled)
            SELECT e.id, b.id, CURRENT_TIMESTAMP, true
            FROM employees e
            JOIN branches b
              ON b.id = :branchId
             AND b.company_id = :companyId
            WHERE e.id = :employeeId
              AND e.company_id = :companyId
            """, nativeQuery = true)
    void insert(@Param("employeeId") Long employeeId, @Param("branchId") Long branchId,
            @Param("companyId") Long companyId);

    // Empleados (activos) de la empresa "con todas las sedes" para auto-asignarles
    // una sede recién
    // creada: los que
    // aún no tienen fila para la sede nueva Y ya cubren TODAS las demás sedes de la
    // empresa
    // (cobertura total). Así,
    // al crear una sede, quien tenía todas la hereda; quien estaba limitado a un
    // subconjunto
    // estricto, no. Caveat:
    // un empleado que casualmente tiene todas las sedes actuales es indistinguible
    // de "todas".
    @Query(value = """
            SELECT e.id
            FROM employees e
            WHERE e.company_id = :companyId
              AND e.enabled = true
              AND NOT EXISTS (SELECT 1 FROM employee_branches eb2 WHERE eb2.employee_id = e.id AND eb2.branch_id = :newBranchId)
              AND NOT EXISTS (SELECT 1 FROM branches b WHERE b.company_id = :companyId AND b.id <> :newBranchId AND NOT EXISTS (SELECT 1 FROM employee_branches eb WHERE eb.employee_id = e.id AND eb.branch_id = b.id AND eb.enabled = true))
            """, nativeQuery = true)
    List<Long> findFullCoverageEmployeeIds(@Param("companyId") Long companyId,
            @Param("newBranchId") Long newBranchId);
}
