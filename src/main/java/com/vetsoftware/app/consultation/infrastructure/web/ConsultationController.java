package com.vetsoftware.app.consultation.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.consultation.application.command.CreateConsultationCommand;
import com.vetsoftware.app.consultation.application.command.UpdateConsultationCommand;
import com.vetsoftware.app.consultation.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.consultation.application.dto.CompanySummaryDto;
import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.consultation.application.dto.ConsultationTypeSummaryDto;
import com.vetsoftware.app.consultation.application.port.in.CreateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.DeleteConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.FindConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.ListConsultationsUseCase;
import com.vetsoftware.app.consultation.application.port.in.ReactivateConsultationUseCase;
import com.vetsoftware.app.consultation.application.port.in.UpdateConsultationUseCase;
import com.vetsoftware.app.consultation.infrastructure.web.request.CreateConsultationRequest;
import com.vetsoftware.app.consultation.infrastructure.web.request.UpdateConsultationRequest;
import com.vetsoftware.app.consultation.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.consultation.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.consultation.infrastructure.web.response.ConsultationResponse;
import com.vetsoftware.app.consultation.infrastructure.web.response.ConsultationTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultations")
public class ConsultationController {
    private final CreateConsultationUseCase createUseCase;
    private final UpdateConsultationUseCase updateUseCase;
    private final FindConsultationUseCase findUseCase;
    private final ListConsultationsUseCase listUseCase;
    private final DeleteConsultationUseCase deleteUseCase;
    private final ReactivateConsultationUseCase reactivateUseCase;
    private final Authz authz;

    public ConsultationController(CreateConsultationUseCase createUseCase,
                                  UpdateConsultationUseCase updateUseCase,
                                  FindConsultationUseCase findUseCase,
                                  ListConsultationsUseCase listUseCase,
                                  DeleteConsultationUseCase deleteUseCase,
                                  ReactivateConsultationUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationResponse create(@Valid @RequestBody CreateConsultationRequest request) {
        return toResponse(createUseCase.execute(
            new CreateConsultationCommand(
                request.date(), request.consultationTypeId(), request.anamnesis(),
                request.diagnosis(), request.therapeuticPlan(), request.diagnosisPlan(),
                request.nextControl(), request.animalId(), authz.currentCompanyId(),
                request.weight(), request.weightUnit())));
    }

    @GetMapping
    public List<ConsultationResponse> listAll() {
        return listUseCase.listAll(authz.currentCompanyId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ConsultationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public ConsultationResponse update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateConsultationRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateConsultationCommand(
                id, request.date(), request.consultationTypeId(), request.anamnesis(),
                request.diagnosis(), request.therapeuticPlan(), request.diagnosisPlan(),
                request.nextControl(), request.animalId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public ConsultationResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private ConsultationResponse toResponse(ConsultationDto dto) {
        ConsultationTypeSummaryDto ct = dto.consultationType();
        AnimalSummaryDto a = dto.animal();
        CompanySummaryDto c = dto.company();
        return new ConsultationResponse(
            dto.id(), dto.date(),
            new ConsultationTypeSummary(ct.id(), ct.name()),
            dto.anamnesis(), dto.diagnosis(), dto.therapeuticPlan(), dto.diagnosisPlan(),
            dto.nextControl(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate(),
            dto.enabled());
    }
}
