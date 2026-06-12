package com.vetsoftware.app.companytaxprofile.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companytaxprofile.application.command.CreateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.command.UpdateCompanyTaxProfileCommand;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanySummaryDto;
import com.vetsoftware.app.companytaxprofile.application.dto.CompanyTaxProfileDto;
import com.vetsoftware.app.companytaxprofile.application.dto.EconomicActivitySummaryDto;
import com.vetsoftware.app.companytaxprofile.application.port.in.CreateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.DeleteCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.FindCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.ReactivateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.application.port.in.UpdateCompanyTaxProfileUseCase;
import com.vetsoftware.app.companytaxprofile.infrastructure.web.request.CreateCompanyTaxProfileRequest;
import com.vetsoftware.app.companytaxprofile.infrastructure.web.request.UpdateCompanyTaxProfileRequest;
import com.vetsoftware.app.companytaxprofile.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.companytaxprofile.infrastructure.web.response.CompanyTaxProfileResponse;
import com.vetsoftware.app.companytaxprofile.infrastructure.web.response.EconomicActivitySummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/company-tax-profile")
public class CompanyTaxProfileController {
    private final CreateCompanyTaxProfileUseCase createUseCase;
    private final UpdateCompanyTaxProfileUseCase updateUseCase;
    private final FindCompanyTaxProfileUseCase findUseCase;
    private final DeleteCompanyTaxProfileUseCase deleteUseCase;
    private final ReactivateCompanyTaxProfileUseCase reactivateUseCase;
    private final Authz authz;

    public CompanyTaxProfileController(CreateCompanyTaxProfileUseCase createUseCase,
                                       UpdateCompanyTaxProfileUseCase updateUseCase,
                                       FindCompanyTaxProfileUseCase findUseCase,
                                       DeleteCompanyTaxProfileUseCase deleteUseCase,
                                       ReactivateCompanyTaxProfileUseCase reactivateUseCase,
                                       Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyTaxProfileResponse create(@Valid @RequestBody CreateCompanyTaxProfileRequest request) {
        return toResponse(createUseCase.execute(new CreateCompanyTaxProfileCommand(
                request.documentType(),
                request.companyDocumentId(),
                request.companyDocumentVerificationDigit(),
                request.legalName(),
                request.taxRegime(),
                request.fiscalEmail(),
                request.commercialName(),
                request.economicActivityId(),
                request.responsibilities(),
                authz.currentCompanyId())));
    }

    @PutMapping
    public CompanyTaxProfileResponse update(@Valid @RequestBody UpdateCompanyTaxProfileRequest request) {
        return toResponse(updateUseCase.execute(new UpdateCompanyTaxProfileCommand(
                request.documentType(),
                request.companyDocumentId(),
                request.companyDocumentVerificationDigit(),
                request.legalName(),
                request.taxRegime(),
                request.fiscalEmail(),
                request.commercialName(),
                request.economicActivityId(),
                request.responsibilities(),
                authz.currentCompanyId())));
    }

    @GetMapping
    public CompanyTaxProfileResponse find() {
        return toResponse(findUseCase.findByCompanyId(authz.currentCompanyId()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete() {
        deleteUseCase.execute(authz.currentCompanyId());
    }

    @PostMapping("/reactivate")
    public CompanyTaxProfileResponse reactivate() {
        return toResponse(reactivateUseCase.execute(authz.currentCompanyId()));
    }

    private CompanyTaxProfileResponse toResponse(CompanyTaxProfileDto dto) {
        CompanySummaryDto c = dto.company();
        EconomicActivitySummaryDto ea = dto.economicActivity();
        return new CompanyTaxProfileResponse(
                dto.id(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.companyDocumentType(),
                dto.companyDocumentId(),
                dto.companyDocumentVerificationDigit(),
                dto.legalName(),
                dto.taxRegime(),
                dto.fiscalEmail(),
                dto.commercialName(),
                ea == null ? null : new EconomicActivitySummary(ea.id(), ea.code(), ea.name()),
                dto.responsibilities(),
                dto.createdDate(),
                dto.enabled());
    }
}
