package com.vetsoftware.app.purchaseorder.application.port.in;

import com.vetsoftware.app.purchaseorder.application.command.ApplyReceiptCommand;
import com.vetsoftware.app.shared.security.NoAuthorizationRequired;

/**
 * Caso de uso INTERNO (sin {@code @PreAuthorize}): lo invoca la feature
 * {@code goodsreceipt} dentro de su propia transacción, ya autorizada — igual
 * que el ledger de inventario. NO se expone por REST.
 */
@NoAuthorizationRequired(reason = "Puerto interno de orquestación: ningún controller lo expone. Corre dentro de la transacción del caso de uso que lo invoca, que ya validó permiso y ownership.")
public interface ReceivePurchaseOrderUseCase {
    /**
     * Aplica una recepción de mercancía: incrementa lo recibido por línea y
     * recalcula el estado de la orden.
     */
    void applyReceipt(ApplyReceiptCommand command);

    /**
     * Revierte una recepción de mercancía: decrementa lo recibido por línea y
     * recalcula el estado de la orden.
     */
    void revertReceipt(ApplyReceiptCommand command);
}
