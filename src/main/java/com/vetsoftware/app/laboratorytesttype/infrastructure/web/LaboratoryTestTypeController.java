package com.vetsoftware.app.laboratorytesttype.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.laboratorytesttype.application.command.CreateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.command.UpdateLaboratoryTestTypeCommand;
import com.vetsoftware.app.laboratorytesttype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.CreateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.DeleteLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.FindLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListAvailableLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ReactivateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.in.UpdateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.infrastructure.web.request.CreateLaboratoryTestTypeRequest;
import com.vetsoftware.app.laboratorytesttype.infrastructure.web.request.UpdateLaboratoryTestTypeRequest;
import com.vetsoftware.app.laboratorytesttype.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.laboratorytesttype.infrastructure.web.response.LaboratoryTestTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratory-test-types")
public class LaboratoryTestTypeController {
    private final CreateLaboratoryTestTypeUseCase createUseCase;
    private final UpdateLaboratoryTestTypeUseCase updateUseCase;
    private final FindLaboratoryTestTypeUseCase findUseCase;
    private final ListLaboratoryTestTypesUseCase listUseCase;
    private final ListAvailableLaboratoryTestTypesUseCase listAvailableUseCase;
    private final DeleteLaboratoryTestTypeUseCase deleteUseCase;
    private final ReactivateLaboratoryTestTypeUseCase reactivateUseCase;
    private final Authz authz;

    public LaboratoryTestTypeController(CreateLaboratoryTestTypeUseCase createUseCase,
            UpdateLaboratoryTestTypeUseCase updateUseCase,
            FindLaboratoryTestTypeUseCase findUseCase, ListLaboratoryTestTypesUseCase listUseCase,
            ListAvailableLaboratoryTestTypesUseCase listAvailableUseCase,
            DeleteLaboratoryTestTypeUseCase deleteUseCase,
            ReactivateLaboratoryTestTypeUseCase reactivateUseCase, Authz authz) {
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
    public LaboratoryTestTypeResponse create(
            @Valid @RequestBody CreateLaboratoryTestTypeRequest request) {
        return toResponse(createUseCase.execute(new CreateLaboratoryTestTypeCommand(request.name(),
                request.description(), authz.currentCompanyId(), request.general())));
    }

    @GetMapping
    public List<LaboratoryTestTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/available")
    public List<LaboratoryTestTypeResponse> listAvailable() {
        return listAvailableUseCase.listAvailable(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public LaboratoryTestTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public LaboratoryTestTypeResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateLaboratoryTestTypeRequest request) {
        return toResponse(
                updateUseCase.execute(new UpdateLaboratoryTestTypeCommand(id, request.name(),
                        request.description(), authz.currentCompanyId(), request.general())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    @PatchMapping("/{id}/enable")
    public LaboratoryTestTypeResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private LaboratoryTestTypeResponse toResponse(LaboratoryTestTypeDto dto) {
        CompanySummaryDto c = dto.company();
        return new LaboratoryTestTypeResponse(dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(), dto.createdDate(), dto.enabled());
    }
}
