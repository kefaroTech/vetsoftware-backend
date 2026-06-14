package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.BuildElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.BuildElectronicDocumentFromAccountUseCase;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "electronicDocument.build")
@Service
public class BuildElectronicDocumentFromAccountService implements BuildElectronicDocumentFromAccountUseCase {
    private final DocumentBuilder documentBuilder;

    public BuildElectronicDocumentFromAccountService(DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(BuildElectronicDocumentCommand command) {
        return ElectronicDocumentDto.from(documentBuilder.build(
                command.openAccountId(), command.documentType(), command.companyId()));
    }
}
