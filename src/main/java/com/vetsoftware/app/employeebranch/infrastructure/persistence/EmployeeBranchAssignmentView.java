package com.vetsoftware.app.employeebranch.infrastructure.persistence;

/**
 * Proyección de una asignación (vigente) empleado→sede junto con el nombre de la sede. La usa el
 * listado de empleados para pintar las sedes de cada uno sin traer el agregado completo de branch.
 */
public interface EmployeeBranchAssignmentView {
  Long getEmployeeId();

  Long getBranchId();

  String getBranchName();
}
