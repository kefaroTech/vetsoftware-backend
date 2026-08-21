package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort.SaleSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.UvtQueryPort;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.springframework.stereotype.Component;

/**
 * Núcleo de construcción del documento PENDIENTE desde una cuenta cerrada.
 * Reutilizable y SIN control de acceso (lo usan la emisión end-to-end de F4, la
 * conversión de POS a factura y la emisión al cerrar la cuenta).
 */
@Component
public class DocumentBuilder {
    private final SaleSnapshotQueryPort saleSnapshotQueryPort;
    private final ElectronicDocumentRepository repository;
    private final PosTicketLimitValidator posTicketLimitValidator;
    private final UvtQueryPort uvtQueryPort;

    public DocumentBuilder(SaleSnapshotQueryPort saleSnapshotQueryPort,
            ElectronicDocumentRepository repository,
            PosTicketLimitValidator posTicketLimitValidator, UvtQueryPort uvtQueryPort) {
        this.saleSnapshotQueryPort = saleSnapshotQueryPort;
        this.repository = repository;
        this.posTicketLimitValidator = posTicketLimitValidator;
        this.uvtQueryPort = uvtQueryPort;
    }

    public ElectronicDocument build(Long openAccountId, ElectronicDocumentType documentType,
            Long companyId, boolean finalConsumer) {
        SaleSnapshot snapshot = saleSnapshotQueryPort.findByOpenAccount(openAccountId, companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Open account not found: " + openAccountId));
        if (!snapshot.accountClosed()) {
            throw new IllegalStateException(
                    "La cuenta debe estar cerrada (CLOSE) para emitir el documento electronico.");
        }
        // Consumidor final: el documento usa la identidad genérica DIAN en vez de los
        // datos del Owner
        // de la cuenta.
        CustomerSnapshot customer = finalConsumer
                ? CustomerSnapshot.finalConsumer()
                : snapshot.customer();
        ElectronicDocument document = ElectronicDocument.createPending(snapshot.companyId(),
                snapshot.openAccountId(), documentType, snapshot.issuer(), customer,
                snapshot.lines(), snapshot.payments(), snapshot.paymentForm(),
                snapshot.customerWithholdingAgent(), snapshot.reteFuenteRate(),
                // La idempotencia del cierre se maneja por cuenta (existsByOpenAccountId), no
                // por
                // client_request_id.
                // issuedByEmployeeId null: la emisión al cerrar la cuenta no tiene un actor
                // fiscal
                // directo aquí;
                // la autoría del cierre queda registrada en OpenAccount.closedBy.
                snapshot.reteIvaRate(), snapshot.reteIcaRate(),
                uvtQueryPort.currentUvt().orElse(null), null, null, snapshot.branchId());
        // 3.2: al cerrar una cuenta como tiquete POS (DOC_EQUIV_POS), tampoco puede
        // superar 5 UVT; por
        // encima
        // exige FE_VENTA (el front ya lo fuerza, esto es el enforcement de backend).
        posTicketLimitValidator.validate(document);
        // El documento nace SIN numerar (PENDIENTE). La numeración fiscal (consecutivo)
        // se asigna en la EMISIÓN (justo antes de transmitir), no aquí: un consecutivo
        // sin transmitir dejaría un hueco que la DIAN penaliza.
        return repository.save(document);
    }
}
