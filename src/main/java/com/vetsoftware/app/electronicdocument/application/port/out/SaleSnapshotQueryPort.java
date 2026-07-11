package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentPayment;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.PaymentForm;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lee de otras features (open account, cargos, perfil fiscal, owner, abonos) y los traduce a un
 * read model ya en tipos de esta feature. El adapter es el unico punto que conoce esas features:
 * deriva snapshots de emisor/adquiriente, mapea cargos no anulados a lineas y abonos a pagos DIAN.
 */
public interface SaleSnapshotQueryPort {
    Optional<SaleSnapshot> findByOpenAccount(Long openAccountId, Long companyId);

    record SaleSnapshot(
            Long companyId,
            Long openAccountId,
            boolean accountClosed,
            IssuerSnapshot issuer,
            CustomerSnapshot customer,
            List<ElectronicDocumentLine> lines,
            List<ElectronicDocumentPayment> payments,
            PaymentForm paymentForm,
            // F6 - retenciones: si el adquiriente es agente retenedor y las tarifas del emisor (cero/null si no aplica).
            boolean customerWithholdingAgent,
            BigDecimal reteFuenteRate,
            BigDecimal reteIvaRate,
            BigDecimal reteIcaRate,
            // Sede de la cuenta cerrada: el documento emitido hereda la sucursal de la cuenta.
            Long branchId
    ) {}
}
