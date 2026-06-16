package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort.SaleSnapshot;
import com.vetsoftware.app.electronicdocument.domain.CustomerSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.springframework.stereotype.Component;

/**
 * Núcleo de construcción del documento PENDIENTE desde una cuenta cerrada. Reutilizable y SIN control
 * de acceso (lo usan el caso de uso de construcción manual y el de emisión end-to-end de F4).
 */
@Component
public class DocumentBuilder {
    private final SaleSnapshotQueryPort saleSnapshotQueryPort;
    private final ElectronicDocumentRepository repository;

    public DocumentBuilder(SaleSnapshotQueryPort saleSnapshotQueryPort,
                           ElectronicDocumentRepository repository) {
        this.saleSnapshotQueryPort = saleSnapshotQueryPort;
        this.repository = repository;
    }

    public ElectronicDocument build(Long openAccountId, ElectronicDocumentType documentType, Long companyId,
                                    boolean finalConsumer) {
        SaleSnapshot snapshot = saleSnapshotQueryPort.findByOpenAccount(openAccountId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Open account not found: " + openAccountId));
        if (!snapshot.accountClosed()) {
            throw new IllegalStateException(
                    "La cuenta debe estar cerrada (CLOSE) para emitir el documento electronico.");
        }
        // Consumidor final: el documento usa la identidad genérica DIAN en vez de los datos del Owner de la cuenta.
        CustomerSnapshot customer = finalConsumer ? CustomerSnapshot.finalConsumer() : snapshot.customer();
        ElectronicDocument document = ElectronicDocument.createPending(
                snapshot.companyId(), snapshot.openAccountId(), documentType,
                snapshot.issuer(), customer, snapshot.lines(), snapshot.payments(),
                snapshot.paymentForm(), null);
        return repository.save(document);
    }
}
