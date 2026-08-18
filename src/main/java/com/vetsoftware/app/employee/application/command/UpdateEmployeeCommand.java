package com.vetsoftware.app.employee.application.command;

/**
 * {@code companyId} no viene del cliente: lo sella el controller desde el
 * contexto autenticado. Nulo significa principal cross-tenant (SYSTEM).
 */
public record UpdateEmployeeCommand(Long id, String employeeCode, String name, String email,
        Long companyId) {
}
