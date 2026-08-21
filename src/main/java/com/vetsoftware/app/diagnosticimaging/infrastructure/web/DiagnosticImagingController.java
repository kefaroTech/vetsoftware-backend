package com.vetsoftware.app.diagnosticimaging.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.diagnosticimaging.application.command.CreateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.command.UpdateDiagnosticImagingCommand;
import com.vetsoftware.app.diagnosticimaging.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.diagnosticimaging.application.dto.CompanySummaryDto;
import com.vetsoftware.app.diagnosticimaging.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingTypeSummaryDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.CreateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.DeleteDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.FindDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsByAnimalUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.in.UpdateDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.request.CreateDiagnosticImagingRequest;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.request.UpdateDiagnosticImagingRequest;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.response.DiagnosticImagingResponse;
import com.vetsoftware.app.diagnosticimaging.infrastructure.web.response.DiagnosticImagingTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diagnostic-imagings")
public class DiagnosticImagingController {
    private final CreateDiagnosticImagingUseCase createUseCase;
    private final UpdateDiagnosticImagingUseCase updateUseCase;
    private final FindDiagnosticImagingUseCase findUseCase;
    private final ListDiagnosticImagingsUseCase listUseCase;
    private final ListDiagnosticImagingsByAnimalUseCase listByAnimalUseCase;
    private final DeleteDiagnosticImagingUseCase deleteUseCase;
    private final Authz authz;

    public DiagnosticImagingController(CreateDiagnosticImagingUseCase createUseCase,
            UpdateDiagnosticImagingUseCase updateUseCase, FindDiagnosticImagingUseCase findUseCase,
            ListDiagnosticImagingsUseCase listUseCase,
            ListDiagnosticImagingsByAnimalUseCase listByAnimalUseCase,
            DeleteDiagnosticImagingUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticImagingResponse create(
            @Valid @RequestBody CreateDiagnosticImagingRequest request) {
        return toResponse(createUseCase.execute(new CreateDiagnosticImagingCommand(request.date(),
                request.diagnosticImagingTypeId(), request.clinicalSigns(), request.studyType(),
                request.diagnosis(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<DiagnosticImagingResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public PageResponse<DiagnosticImagingResponse> listByAnimal(@PathVariable Long animalId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByAnimalUseCase.listByAnimal(animalId,
                authz.currentCompanyId(), query, page, pageSize), this::toResponse);
    }

    @GetMapping("/{id}")
    public DiagnosticImagingResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public DiagnosticImagingResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateDiagnosticImagingRequest request) {
        return toResponse(updateUseCase.execute(new UpdateDiagnosticImagingCommand(id,
                request.date(), request.diagnosticImagingTypeId(), request.clinicalSigns(),
                request.studyType(), request.diagnosis(), request.observations(),
                request.animalId(), request.consultationId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private DiagnosticImagingResponse toResponse(DiagnosticImagingDto dto) {
        DiagnosticImagingTypeSummaryDto t = dto.diagnosticImagingType();
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new DiagnosticImagingResponse(dto.id(), dto.date(),
                new DiagnosticImagingTypeSummary(t.id(), t.name()), dto.clinicalSigns(),
                dto.studyType(), dto.diagnosis(), dto.observations(), dto.status(),
                new AnimalSummary(a.id(), a.name(), a.code()),
                co == null ? null : new ConsultationSummary(co.id(), co.date()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled());
    }
}
