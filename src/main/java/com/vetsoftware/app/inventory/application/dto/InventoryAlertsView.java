package com.vetsoftware.app.inventory.application.dto;

import java.util.List;

/**
 * Alertas de inventario de una sede (o todas): productos bajo mínimo + lotes por vencer/vencidos.
 */
public record InventoryAlertsView(List<StockView> lowStock, List<ExpiringLotView> expiring) {}
