package com.vetsoftware.app.company.application.usecase;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.ProvisionCompanyUseCase;
import com.vetsoftware.app.company.application.port.out.CompanyCreationPort;
import com.vetsoftware.app.company.application.port.out.InitialContractProvisioningPort;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Impide que el endpoint de empresas deje una fila huérfana: empresa, contrato
 * inicial y entitlements participan en la misma transacción.
 */
@Observed(name = "company.provision")
@Service
public class ProvisionCompanyService implements ProvisionCompanyUseCase {

    private final CompanyCreationPort companyCreationPort;
    private final InitialContractProvisioningPort initialContractProvisioningPort;

    public ProvisionCompanyService(CompanyCreationPort companyCreationPort,
            InitialContractProvisioningPort initialContractProvisioningPort) {
        this.companyCreationPort = companyCreationPort;
        this.initialContractProvisioningPort = initialContractProvisioningPort;
    }

    @Override
    @Transactional
    public CompanyDto execute(CreateCompanyCommand command) {
        CompanyDto company = companyCreationPort.create(command);
        initialContractProvisioningPort.provisionForCompany(company.id());
        return company;
    }
}
