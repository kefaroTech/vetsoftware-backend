package com.vetsoftware.app.employeebranch.infrastructure.web.request;

import java.util.List;

/**
 * Set atómico de sedes de un empleado. {@code allBranches=true} asigna todas las de la empresa
 * (ignora {@code branchIds}); si es false, asigna exactamente {@code branchIds}.
 */
public record SetEmployeeBranchesRequest(boolean allBranches, List<Long> branchIds) {}
