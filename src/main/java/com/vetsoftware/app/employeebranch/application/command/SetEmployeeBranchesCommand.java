package com.vetsoftware.app.employeebranch.application.command;

import java.util.List;

/**
 * Set atómico de sedes de un empleado. Si {@code allBranches} es true se ignora {@code branchIds} y
 * se asignan TODAS las sedes de la empresa (materializadas como filas); si es false, se asigna
 * exactamente {@code branchIds}.
 */
public record SetEmployeeBranchesCommand(
    Long employeeId, Long companyId, boolean allBranches, List<Long> branchIds) {}
