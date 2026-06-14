package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.BuildElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.SaleSnapshotQueryPort.SaleSnapshot;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "electronicDocument.build")
@Service
public class BuildElectronicDocumentFromAccountService implements BuildElectronicDocumentFromAccountUseCase {
    private final SaleSnapshotQueryPort saleSnapshotQueryPort;
    private final ElectronicDocumentRepository repository;

    public BuildElectronicDocumentFromAccountService(SaleSnapshotQueryPort saleSnapshotQueryPort,
                                                     ElectronicDocumentRepository repository) {
        this.saleSnapshotQueryPort = saleSnapshotQueryPort;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(BuildElectronicDocumentCommand command) {
        SaleSnapshot snapshot = saleSnapshotQueryPort
                .findByOpenAccount(command.openAccountId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Open account not found: " + command.openAccountId()));
        if (!snapshot.accountClosed()) {
            throw new IllegalStateException(
                    "La cuenta debe estar cerrada (CLOSE) para emitir el documento electronico.");
        }
        ElectronicDocument document = ElectronicDocument.createPending(
                snapshot.companyId(), snapshot.openAccountId(), command.documentType(),
                snapshot.issuer(), snapshot.customer(), snapshot.lines(), snapshot.payments(),
                snapshot.paymentForm(), null);
        return ElectronicDocumentDto.from(repository.save(document));
    }
}
