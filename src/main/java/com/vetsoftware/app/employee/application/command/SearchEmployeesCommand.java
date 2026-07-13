package com.vetsoftware.app.employee.application.command;

/**
 * Búsqueda paginada de empleados de una empresa. {@code query} es opcional (busca por nombre, código o correo,
 * case-insensitive); {@code companyId} lo inyecta el controller desde el JWT. Incluye desactivados.
 */
public record SearchEmployeesCommand(
        Long companyId,
        String query,
        int page,
        int pageSize
) {}
