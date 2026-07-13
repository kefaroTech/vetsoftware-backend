package com.vetsoftware.app.inventory.application.command;

/** Consulta de valuación. {@code branchId} null = todas las sedes accesibles. */
public record InventoryValuationQuery(Long companyId, Long branchId) {}
