package com.vetsoftware.app.inventory.application.command;

/**
 * Transferencia de existencias entre dos sedes de la misma empresa (salida FEFO en origen + entrada
 * en destino).
 */
public record TransferStockCommand(
    Long companyId,
    Long fromBranchId,
    Long toBranchId,
    Long productId,
    int quantity,
    String reason,
    Long createdBy) {}
