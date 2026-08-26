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
    private final Authz authz;

    public DiagnosticImagingTypeController(CreateDiagnosticImagingTypeUseCase createUseCase,
            UpdateDiagnosticImagingTypeUseCase updateUseCase,
            FindDiagnosticImagingTypeUseCase findUseCase,
            ListDiagnosticImagingTypesUseCase listUseCase,
            ListAvailableDiagnosticImagingTypesUseCase listAvailableUseCase,
            DeleteDiagnosticImagingTypeUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listAvailableUseCase = listAvailableUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    /**
     * La empresa sale de {@code currentCompanyIdOrNull()} y no de
     * {@code currentCompanyId()}, que es el arreglo de #565. Con la segunda, un
     * principal de plataforma —que no tiene empresa— moría con un
     * {@code AccessDeniedException} sin contexto, y un empleado recibía siempre su
     * empresa, así que {@code general = true} chocaba contra el XOR del dominio:
     * NINGÚN actor podía crear un tipo global, pese a que el {@code @PreAuthorize}
     * del caso de uso abre explícitamente a {@code hasRole('SYSTEM')} y el propio
     * servicio ya tiene escrito el camino {@code companyId == null}. Es la misma
     * herramienta que el {@code delete} de este controller ya usaba.
     *
     * <p>
     * El request sigue sin poder declarar la empresa: para un empleado la sigue
     * poniendo el contexto, y {@code null} solo lo produce un principal que no
     * tiene ninguna.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticImagingTypeResponse create(
            @Valid @RequestBody CreateDiagnosticImagingTypeRequest request) {
        return toResponse(
                createUseCase.execute(new CreateDiagnosticImagingTypeCommand(request.name(),
                        request.description(), authz.currentCompanyIdOrNull(), request.general())));
    }

    @GetMapping
    public List<DiagnosticImagingTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/available")
    public List<DiagnosticImagingTypeResponse> listAvailable() {
        return listAvailableUseCase.listAvailable(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DiagnosticImagingTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public DiagnosticImagingTypeResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateDiagnosticImagingTypeRequest request) {
        return toResponse(
                updateUseCase.execute(new UpdateDiagnosticImagingTypeCommand(id, request.name(),
                        request.description(), authz.currentCompanyIdOrNull(), request.general())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private DiagnosticImagingTypeResponse toResponse(DiagnosticImagingTypeDto dto) {
        CompanySummaryDto c = dto.company();
        return new DiagnosticImagingTypeResponse(dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(), dto.createdDate(), dto.enabled());
    }
}
