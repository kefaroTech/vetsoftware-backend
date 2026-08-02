package com.vetsoftware.app.inventory.application.command;

import java.util.List;

/**
 * Conteo físico de inventario en una sede: por cada línea, el servicio compara {@code
 * countedQuantity} contra el saldo de sistema y genera el ajuste por la diferencia. {@code
 * countedBy} = empleado que contó (nullable).
 */
public record RecordCountCommand(
    Long companyId, Long branchId, String note, Long countedBy, List<Line> lines) {

  /** Una línea de la hoja de conteo: cuántas unidades se contaron físicamente de un producto. */
  public record Line(Long productId, int countedQuantity) {}
}
