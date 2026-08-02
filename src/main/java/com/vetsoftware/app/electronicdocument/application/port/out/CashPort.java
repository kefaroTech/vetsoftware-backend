package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import java.math.BigDecimal;
import java.util.List;

/**
 * Afecta la caja (feature {@code cashregister}) por una venta POS directa. SOLO
 * para documentos POS (sin cuenta abierta): las ventas por cuenta ya movieron
 * caja como abono. El movimiento entra en la sesión OPEN de la sede (SALE_IN
 * por método, ref {@code POS_DOCUMENT}), idempotente por documento+método. El
 * adapter de orquestación traduce {@code PaymentMeans} → medio de caja y llama
 * al caso de uso de {@code cashregister}.
 */
public interface CashPort {

    /**
     * Bloqueo "caja requerida": si la empresa lo exige y la sede no tiene caja
     * OPEN, lanza {@code
     * NoOpenCashSessionException} (→ 409 NO_OPEN_CASH_SESSION) ANTES de emitir.
     * No-op si no se exige.
     */
    void requireOpenSession(Long companyId, Long branchId, Long employeeId);

    /**
     * Registra el cobro de la venta POS en la caja OPEN de la sede. Idempotente.
     * No-op si no hay caja abierta.
     */
    void registerSale(Long companyId, Long branchId, Long documentId, List<PaymentLine> payments,
            Long employeeId);

    /**
     * Compensa (VOID_OUT) en la caja OPEN actual el cobro de una venta POS anulada
     * por nota crédito. Idempotente.
     */
    void reverseSale(Long companyId, Long branchId, Long documentId, List<PaymentLine> payments,
            Long actorId);

    /**
     * Un pago del documento POS: medio DIAN + monto. El adapter lo mapea al medio
     * de caja.
     */
    record PaymentLine(PaymentMeans paymentMeans, BigDecimal amount) {
    }
}
