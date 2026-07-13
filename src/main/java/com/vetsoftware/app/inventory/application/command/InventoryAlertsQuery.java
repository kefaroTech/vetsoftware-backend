package com.vetsoftware.app.inventory.application.command;

/** Consulta de alertas. {@code branchId} null = todas las sedes accesibles; {@code expiringInDays} umbral de vencimiento. */
public record InventoryAlertsQuery(Long companyId, Long branchId, int expiringInDays) {}
