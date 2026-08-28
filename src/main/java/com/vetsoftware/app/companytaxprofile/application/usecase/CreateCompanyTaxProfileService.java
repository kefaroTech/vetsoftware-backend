package com.vetsoftware.app.companytaxprofile.application.usecase;

import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.companytaxprofile.application.port.out.CompanyTaxProfileRepository;
import com.vetsoftware.app.companytaxprofile.application.port.out.EconomicActivityQueryPort;
import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileAlreadyExistsException;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.companytaxprofile.domain.NitVerificationDigit;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "company.tax.profile.create")
@Service
public class CreateCompanyTaxProfileService implements CreateCompanyTaxProfileUseCase {
    private final CompanyTaxProfileRepository repository;
    private final CompanyQueryPort companyQueryPort;
    private final EconomicActivityQueryPort economicActivityQueryPort;
    private final Clock clock;

    public CreateCompanyTaxProfileService(CompanyTaxProfileRepository repository,
            CompanyQueryPort companyQueryPort, EconomicActivityQueryPort economicActivityQueryPort,
            Clock clock) {
        this.repository = repository;
        this.companyQueryPort = companyQueryPort;
        this.economicActivityQueryPort = economicActivityQueryPort;
        this.clock = clock;
    }

    @Override
    public CompanyTaxProfileDto execute(CreateCompanyTaxProfileCommand command) {
        CompanyRef company = companyQueryPort.findById(command.companyId()).orElseThrow(
                () -> new IllegalArgumentException("Company not found: " + command.companyId()));
        // VIGENTE, no "alguno": desde el changeset 364 la tabla guarda historico, y
        // una empresa con perfiles cerrados y ninguno abierto si puede volver a abrir.
        if (repository.existsCurrentByCompanyId(command.companyId())) {
            throw new CompanyTaxProfileAlreadyExistsException(command.companyId());
        }
        EconomicActivityRef economicActivity = command.economicActivityId() == null
                ? null
                : economicActivityQueryPort.findById(command.economicActivityId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Economic activity not found: " + command.economicActivityId()));
        List<CompanyTaxProfileResponsibility> responsibilities = toResponsibilities(
                command.responsibilityCodes());
        // El DV del NIT es determinístico (módulo 11): se autocalcula y es
        // autoritativo, ignorando
        // cualquier
        // valor entrante. Para otros tipos de documento no aplica DV.
        String verificationDigit = command.companyDocumentType() == CompanyDocumentType.NIT
                ? NitVerificationDigit.calculate(command.companyDocumentId())
                : null;
        // Reloj inyectado con zona: la vigencia del perfil es un dato fiscal y su
        // primer dia no lo puede decidir la zona por defecto de la JVM.
        CompanyTaxProfile profile = CompanyTaxProfile.open(company, command.companyDocumentType(),
                command.companyDocumentId(), verificationDigit, command.legalName(),
                command.taxRegime(), command.fiscalEmail(), command.commercialName(),
                economicActivity, responsibilities, LocalDate.now(clock), LocalDateTime.now(clock));
        return CompanyTaxProfileDto.from(repository.save(profile));
    }

    private static List<CompanyTaxProfileResponsibility> toResponsibilities(List<String> codes) {
        if (codes == null)
            return List.of();
        return codes.stream().map(CompanyTaxProfileResponsibility::new).toList();
    }
}
