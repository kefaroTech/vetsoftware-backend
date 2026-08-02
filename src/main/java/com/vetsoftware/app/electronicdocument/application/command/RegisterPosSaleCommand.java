package com.vetsoftware.app.electronicdocument.application.command;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.electronicdocument.domain.PaymentMeans;
import java.math.BigDecimal;
import java.util.List;

/**
 * Venta de POS a registrar como documento electronico (sin cuenta abierta). Empresas con BILLING lo
 * transmiten a la DIAN; sin el modulo el documento queda PENDIENTE (datos guardados, emision diferida).
 * El companyId lo inyecta el controller desde el contexto (el front nunca lo manda).
 */
public record RegisterPosSaleCommand(
        Long companyId,
        ElectronicDocumentType documentType,
        boolean finalConsumer,
        /** null o finalConsumer=true => consumidor final anonimo. */
        Long customerOwnerId,
        List<SaleLine> lines,
        List<SalePayment> payments,
        /** Idempotencia: UUID por apertura del cobro; null en clientes legacy que no lo envían. */
        String clientRequestId,
        /** Actor fiscal: empleado que registra la venta, inyectado por el controller desde el contexto auth. */
        Long issuedByEmployeeId,
        /** Sede emisora (opcional). Si no viene, se resuelve a la "Principal" de la empresa. */
        Long branchId
) {
    /** true si alguna linea es GENERAL (precio libre): solo SYSTEM puede emitirlas (ver el gate del caso de uso). */
    public boolean hasGeneralLine() {
        return lines != null && lines.stream().anyMatch(l -> l.kind() == SaleLineKind.GENERAL);
    }

    /** Linea de venta: el unitPrice es el precio final (post-promo) con IVA incluido. */
    public record SaleLine(SaleLineKind kind, Long refId, String description,
                           BigDecimal quantity, BigDecimal unitPrice) {}

    public record SalePayment(PaymentMeans means, BigDecimal amount) {}
}
