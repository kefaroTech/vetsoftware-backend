package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.registration.application.port.out.CompanyTaxProfileCreator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Único punto de orquestación que conoce la feature companytaxprofile. Crea el perfil fiscal del emisor
 * durante el signup público con el tipo de documento elegido, DV autocalculado por el service (para NIT)
 * y sin responsabilidades.
 */
@Component
public class CreateCompanyTaxProfileAdapter implements CompanyTaxProfileCreator {

    private final CreateCompanyTaxProfileUseCase createCompanyTaxProfileUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public CreateCompanyTaxProfileAdapter(CreateCompanyTaxProfileUseCase createCompanyTaxProfileUseCase,
                                          SystemAuthRunner systemAuthRunner) {
        this.createCompanyTaxProfileUseCase = createCompanyTaxProfileUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void create(Long companyId, String documentType, String documentId, String legalName,
                       String taxRegime, String fiscalEmail) {
        systemAuthRunner.call(() -> createCompanyTaxProfileUseCase.execute(new CreateCompanyTaxProfileCommand(
            CompanyDocumentType.valueOf(documentType),
            documentId,
            null,                       // DV: lo autocalcula el service para NIT
            legalName,
            TaxRegime.valueOf(taxRegime),
            fiscalEmail,
            null,                       // commercialName
            null,                       // economicActivityId
            List.of(),                  // sin responsabilidades
            companyId
        )));
    }
}
