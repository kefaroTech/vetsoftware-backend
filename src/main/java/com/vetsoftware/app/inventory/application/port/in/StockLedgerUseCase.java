package com.vetsoftware.app.inventory.application.port.in;

import com.vetsoftware.app.inventory.application.command.RecordAdjustmentCommand;
import com.vetsoftware.app.inventory.application.command.RecordClinicalUseCommand;
import com.vetsoftware.app.inventory.application.command.RecordPurchaseCommand;
import com.vetsoftware.app.inventory.application.command.RecordSaleCommand;
import com.vetsoftware.app.inventory.application.command.TransferStockCommand;
import com.vetsoftware.app.inventory.application.dto.StockConsumptionDto;
import com.vetsoftware.app.inventory.domain.StockReferenceType;
import java.util.List;

/**
 * Único punto de afectación de inventario. Lo consumen POS, cuenta abierta y clínica (vía adapters
 * de orquestación) y los endpoints de ajuste. Todas las operaciones son transaccionales y por sede.
 */
public interface StockLedgerUseCase {

  /** Entrada de inventario (compra/recepción). */
  void recordPurchase(RecordPurchaseCommand command);

  /**
   * Salida por venta/consumo (FEFO). Idempotente por (referenceType, referenceId). Devuelve el
   * consumo por lote.
   */
  List<StockConsumptionDto> recordSale(RecordSaleCommand command);

  /**
   * Consumo clínico (FEFO, tipo CLINICAL_USE, ref CLINICAL_EVENT). Respeta el flag de stock
   * negativo.
   */
  List<StockConsumptionDto> recordClinicalUse(RecordClinicalUseCommand command);

  /** Ajuste por conteo físico (delta con signo, motivo obligatorio). */
  void recordAdjustment(RecordAdjustmentCommand command);

  /**
   * Transferencia entre sedes: salida FEFO en origen + entrada en destino (preserva
   * lote/costo/vencimiento).
   */
  void transfer(TransferStockCommand command);

  /** Compensa (anula) todos los movimientos de una referencia con su inverso. Idempotente. */
  void reverse(StockReferenceType referenceType, Long referenceId, Long createdBy);
}
