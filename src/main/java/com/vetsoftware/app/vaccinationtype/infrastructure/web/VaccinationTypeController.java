package com.vetsoftware.app.vaccinationtype.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.CreateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.FindVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListAvailableVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ReactivateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.request.CreateVaccinationTypeRequest;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.request.UpdateVaccinationTypeRequest;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.response.VaccinationTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vaccination-types")
public class VaccinationTypeController {
    private final CreateVaccinationTypeUseCase createUseCase;
    private final UpdateVaccinationTypeUseCase updateUseCase;
    private final FindVaccinationTypeUseCase findUseCase;
    private final ListVaccinationTypesUseCase listUseCase;
    private final ListAvailableVaccinationTypesUseCase listAvailableUseCase;
    private final DeleteVaccinationTypeUseCase deleteUseCase;
    private final ReactivateVaccinationTypeUseCase reactivateUseCase;
    private final Authz authz;

    public VaccinationTypeController(CreateVaccinationTypeUseCase createUseCase,
            UpdateVaccinationTypeUseCase updateUseCase, FindVaccinationTypeUseCase findUseCase,
            ListVaccinationTypesUseCase listUseCase,
            ListAvailableVaccinationTypesUseCase listAvailableUseCase,
            DeleteVaccinationTypeUseCase deleteUseCase,
            ReactivateVaccinationTypeUseCase reactivateUseCase, Authz authz) {
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
    public VaccinationTypeResponse create(
            @Valid @RequestBody CreateVaccinationTypeRequest request) {
        return toResponse(createUseCase.execute(new CreateVaccinationTypeCommand(request.name(),
                request.description(), authz.currentCompanyId(), request.general())));
    }

    @GetMapping
    public List<VaccinationTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/available")
    public List<VaccinationTypeResponse> listAvailable() {
        return listAvailableUseCase.listAvailable(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VaccinationTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public VaccinationTypeResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateVaccinationTypeRequest request) {
        return toResponse(updateUseCase.execute(new UpdateVaccinationTypeCommand(id, request.name(),
                request.description(), authz.currentCompanyId(), request.general())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public VaccinationTypeResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private VaccinationTypeResponse toResponse(VaccinationTypeDto dto) {
        CompanySummaryDto c = dto.company();
        return new VaccinationTypeResponse(dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(), dto.createdDate(), dto.enabled());
    }
}
