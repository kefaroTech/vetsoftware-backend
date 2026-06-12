package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "companyTaxProfile.create")
@Service
public class CreateCompanyTaxProfileService implements CreateCompanyTaxProfileUseCase {
    private final CompanyTaxProfileRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final EconomicActivityQueryPort economicActivityQueryPort;

    public CreateCompanyTaxProfileService(CompanyTaxProfileRepository repository,
                                          CompanyQueryPort companyQueryPort,
                                          EconomicActivityQueryPort economicActivityQueryPort) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.economicActivityQueryPort = economicActivityQueryPort;
    }

    @Override
    public CompanyTaxProfileDto execute(CreateCompanyTaxProfileCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + command.companyId()));
        if (repository.existsByCompanyId(command.companyId())) {
            throw new CompanyTaxProfileAlreadyExistsException(command.companyId());
        }
        EconomicActivityRef economicActivity = command.economicActivityId() == null ? null
                : economicActivityQueryPort.findById(command.economicActivityId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Economic activity not found: " + command.economicActivityId()));
        List<CompanyTaxProfileResponsibility> responsibilities = toResponsibilities(command.responsibilityCodes());
        CompanyTaxProfile profile = CompanyTaxProfile.create(
                company,
                command.companyDocumentType(),
                command.companyDocumentId(),
                command.companyDocumentVerificationDigit(),
                command.legalName(),
                command.taxRegime(),
                command.fiscalEmail(),
                command.commercialName(),
                economicActivity,
                responsibilities);
        return CompanyTaxProfileDto.from(repository.save(profile));
    }

    private static List<CompanyTaxProfileResponsibility> toResponsibilities(List<String> codes) {
        if (codes == null) return List.of();
        return codes.stream().map(CompanyTaxProfileResponsibility::new).toList();
    }
}
