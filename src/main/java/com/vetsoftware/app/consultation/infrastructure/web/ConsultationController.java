package com.vetsoftware.app.consultation.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.consultation.application.dto.CompanySummaryDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.dto.ConsultationTypeSummaryDto;
import com.vetsoftware.app.consultation.application.port.in.CreateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.FindConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.infrastructure.web.request.CreateConsultationRequest;
import com.vetsoftware.app.consultation.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.consultation.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.consultation.infrastructure.web.response.ConsultationResponse;
import com.vetsoftware.app.consultation.infrastructure.web.response.ConsultationTypeSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultations")
public class ConsultationController {
    private final CreateConsultationUseCase createUseCase;
    private final FindConsultationUseCase findUseCase;
    private final ListConsultationsUseCase listUseCase;
    private final Authz authz;

    public ConsultationController(CreateConsultationUseCase createUseCase,
            FindConsultationUseCase findUseCase, ListConsultationsUseCase listUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationResponse create(@Valid @RequestBody CreateConsultationRequest request) {
        return toResponse(createUseCase.execute(new CreateConsultationCommand(request.date(),
                request.consultationTypeId(), request.anamnesis(), request.diagnosis(),
                request.prognosis(), request.nextControl(), request.animalId(),
                authz.currentCompanyId(), request.weight(), request.weightUnit(),
                request.temperature(), request.heartRate(), request.respiratoryRate(),
                request.mucousMembranes(), request.capillaryRefill(), request.hydration(),
                request.bodyConditionScore(), request.painScore(), request.attitude(),
                request.examFindings())));
    }

    @GetMapping
    public PageResponse<ConsultationResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}")
    public ConsultationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    private ConsultationResponse toResponse(ConsultationDto dto) {
        ConsultationTypeSummaryDto ct = dto.consultationType();
        AnimalSummaryDto a = dto.animal();
        CompanySummaryDto c = dto.company();
        return new ConsultationResponse(dto.id(), dto.date(),
                new ConsultationTypeSummary(ct.id(), ct.name()), dto.anamnesis(), dto.diagnosis(),
                dto.prognosis(), dto.nextControl(), new AnimalSummary(a.id(), a.name(), a.code()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled(), dto.temperature(), dto.heartRate(), dto.respiratoryRate(),
                dto.mucousMembranes(), dto.capillaryRefill(), dto.hydration(),
                dto.bodyConditionScore(), dto.painScore(), dto.attitude(), dto.examFindings());
    }
}
