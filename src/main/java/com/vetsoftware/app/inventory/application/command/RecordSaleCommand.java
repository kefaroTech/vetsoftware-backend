package com.vetsoftware.app.inventory.application.command;

import com.vetsoftware.app.inventory.domain.StockReferenceType;

/**
 * Salida de inventario por venta/consumo: descuenta FEFO de los lotes de la sede.
 *
 * <p>{@code allowNegative}: si es {@code true}, la salida se permite aunque no haya existencias, sin consultar la
 * política de la empresa (caso POS: la venta de mostrador nunca se frena por stock). Si es {@code false}, respeta el
 * flag {@code inventory.allow_negative_stock} (caso cuenta abierta): bloquea con {@code InsufficientStockException}.
 */
public record RecordSaleCommand(Long companyId, Long branchId, Long productId, int quantity,
                                StockReferenceType referenceType, Long referenceId, Long createdBy,
                                boolean allowNegative) {}
