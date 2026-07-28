package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.UpdateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileNotFoundException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.NitVerificationDigit;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "company.tax.profile.update")
@Service
public class UpdateCompanyTaxProfileService implements UpdateCompanyTaxProfileUseCase {
    private final CompanyTaxProfileRepository repository;
    private final EconomicActivityQueryPort economicActivityQueryPort;

    public UpdateCompanyTaxProfileService(CompanyTaxProfileRepository repository,
                                          EconomicActivityQueryPort economicActivityQueryPort) {
        this.repository = repository;
        this.economicActivityQueryPort = economicActivityQueryPort;
    }

    @Override
    @Transactional
    public CompanyTaxProfileDto execute(UpdateCompanyTaxProfileCommand command) {
        CompanyTaxProfile profile = repository.findByCompanyId(command.companyId())
                .orElseThrow(() -> new CompanyTaxProfileNotFoundException(command.companyId()));
        EconomicActivityRef economicActivity = command.economicActivityId() == null ? null
                : economicActivityQueryPort.findById(command.economicActivityId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Economic activity not found: " + command.economicActivityId()));
        List<CompanyTaxProfileResponsibility> responsibilities = toResponsibilities(command.responsibilityCodes());
        // El DV del NIT es determinístico (módulo 11): se autocalcula y es autoritativo, ignorando cualquier
        // valor entrante. Para otros tipos de documento no aplica DV.
        String verificationDigit = command.companyDocumentType() == CompanyDocumentType.NIT
                ? NitVerificationDigit.calculate(command.companyDocumentId())
                : null;
        profile.update(
                command.companyDocumentType(),
                command.companyDocumentId(),
                verificationDigit,
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
