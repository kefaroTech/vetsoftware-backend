package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.FindDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListAvailableDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ReactivateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request.CreateDiagnosticImagingTypeRequest;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request.UpdateDiagnosticImagingTypeRequest;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.response.DiagnosticImagingTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/diagnostic-imaging-types")
public class DiagnosticImagingTypeController {
    private final CreateDiagnosticImagingTypeUseCase createUseCase;
    private final UpdateDiagnosticImagingTypeUseCase updateUseCase;
    private final FindDiagnosticImagingTypeUseCase findUseCase;
    private final ListDiagnosticImagingTypesUseCase listUseCase;
    private final ListAvailableDiagnosticImagingTypesUseCase listAvailableUseCase;
    private final DeleteDiagnosticImagingTypeUseCase deleteUseCase;
    private final ReactivateDiagnosticImagingTypeUseCase reactivateUseCase;
    private final Authz authz;

    public DiagnosticImagingTypeController(CreateDiagnosticImagingTypeUseCase createUseCase,
                                           UpdateDiagnosticImagingTypeUseCase updateUseCase,
                                           FindDiagnosticImagingTypeUseCase findUseCase,
                                           ListDiagnosticImagingTypesUseCase listUseCase,
                                           ListAvailableDiagnosticImagingTypesUseCase listAvailableUseCase,
                                           DeleteDiagnosticImagingTypeUseCase deleteUseCase,
                                           ReactivateDiagnosticImagingTypeUseCase reactivateUseCase,
                                           Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listAvailableUseCase = listAvailableUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticImagingTypeResponse create(@Valid @RequestBody CreateDiagnosticImagingTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateDiagnosticImagingTypeCommand(request.name(), request.description(),
                        request.companyId(), request.general())));
    }

    @GetMapping
    public List<DiagnosticImagingTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/available")
    public List<DiagnosticImagingTypeResponse> listAvailable() {
        return listAvailableUseCase.listAvailable(authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DiagnosticImagingTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public DiagnosticImagingTypeResponse update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateDiagnosticImagingTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateDiagnosticImagingTypeCommand(id, request.name(), request.description(),
                        request.companyId(), request.general())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public DiagnosticImagingTypeResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private DiagnosticImagingTypeResponse toResponse(DiagnosticImagingTypeDto dto) {
        CompanySummaryDto c = dto.company();
        return new DiagnosticImagingTypeResponse(
                dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(),
                dto.createdDate(), dto.enabled());
    }
}
