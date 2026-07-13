package com.vetsoftware.app.inventory.application.command;

/** Filtro de listado de sesiones de conteo por empresa y (opcional) sede, paginado. */
public record SearchCountsQuery(Long companyId, Long branchId, int page, int pageSize) {}
