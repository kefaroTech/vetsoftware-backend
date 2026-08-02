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
    @Query("select eb.branch.id from EmployeeBranchJpaEntity eb where eb.employee.id = :employeeId")
    List<Long> findBranchIdsByEmployeeId(Long employeeId);

    // Asignaciones vigentes (con nombre de la sede) de un conjunto de empleados,
    // para pintar las
    // sedes en el
    // listado/detalle. El @SQLRestriction("enabled = true") de la entidad ya
    // excluye asignaciones
    // desactivadas.
    @Query("select eb.employee.id as employeeId, eb.branch.id as branchId, eb.branch.name as branchName "
            + "from EmployeeBranchJpaEntity eb where eb.employee.id in :employeeIds")
    List<EmployeeBranchAssignmentView> findAssignmentsByEmployeeIds(
            @Param("employeeIds") List<Long> employeeIds);

    // Soft-delete de TODAS las asignaciones vigentes del empleado (primer paso del
    // set atómico).
    // Nativa para saltar
    // el @SQLRestriction y actualizar por employee_id directamente.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employee_branches SET enabled = false WHERE employee_id = :employeeId AND enabled"
            + " = true", nativeQuery = true)
    void disableAllByEmployeeId(@Param("employeeId") Long employeeId);

    // Reactiva una asignación previamente desactivada (evita insertar un duplicado
    // que violaría el
    // unique). Devuelve
    // el nº de filas afectadas: 0 → no existía esa pareja, hay que insertarla.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "UPDATE employee_branches SET enabled = true "
            + "WHERE employee_id = :employeeId AND branch_id = :branchId AND enabled = false", nativeQuery = true)
    int reactivate(@Param("employeeId") Long employeeId, @Param("branchId") Long branchId);

    // Inserta una asignación nueva. Solo se llama cuando reactivate devolvió 0 (no
    // había fila), así
    // que no choca con
    // el unique (employee_id, branch_id).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = "INSERT INTO employee_branches (employee_id, branch_id, created_date, enabled) "
            + "VALUES (:employeeId, :branchId, CURRENT_TIMESTAMP, true)", nativeQuery = true)
    void insert(@Param("employeeId") Long employeeId, @Param("branchId") Long branchId);

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
    @Query(value = "SELECT e.id FROM employees e WHERE e.company_id = :companyId AND e.enabled = true AND"
            + " NOT EXISTS (SELECT 1 FROM employee_branches eb2                 WHERE"
            + " eb2.employee_id = e.id AND eb2.branch_id = :newBranchId) AND NOT EXISTS (SELECT 1"
            + " FROM branches b                 WHERE b.company_id = :companyId AND b.id <>"
            + " :newBranchId                 AND NOT EXISTS (SELECT 1 FROM employee_branches eb  "
            + "                               WHERE eb.employee_id = e.id AND eb.branch_id = b.id"
            + " AND eb.enabled = true))", nativeQuery = true)
    List<Long> findFullCoverageEmployeeIds(@Param("companyId") Long companyId,
            @Param("newBranchId") Long newBranchId);
}
