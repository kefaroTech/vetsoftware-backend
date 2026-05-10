package com.vetsoftware.app.diagnosticimagingtype.infrastructure.web;

import com.vetsoftware.app.diagnosticimagingtype.application.command.CreateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.command.UpdateDiagnosticImagingTypeCommand;
import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.CreateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.DeleteDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.FindDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.ListDiagnosticImagingTypesUseCase;
import com.vetsoftware.app.diagnosticimagingtype.application.port.in.UpdateDiagnosticImagingTypeUseCase;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request.CreateDiagnosticImagingTypeRequest;
import com.vetsoftware.app.diagnosticimagingtype.infrastructure.web.request.UpdateDiagnosticImagingTypeRequest;
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
    private final DeleteDiagnosticImagingTypeUseCase deleteUseCase;

    public DiagnosticImagingTypeController(CreateDiagnosticImagingTypeUseCase createUseCase,
                                           UpdateDiagnosticImagingTypeUseCase updateUseCase,
                                           FindDiagnosticImagingTypeUseCase findUseCase,
                                           ListDiagnosticImagingTypesUseCase listUseCase,
                                           DeleteDiagnosticImagingTypeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiagnosticImagingTypeResponse create(@Valid @RequestBody CreateDiagnosticImagingTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateDiagnosticImagingTypeCommand(request.name(), request.description())));
    }

    @GetMapping
    public List<DiagnosticImagingTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DiagnosticImagingTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public DiagnosticImagingTypeResponse update(@PathVariable Long id,
                                                @Valid @RequestBody UpdateDiagnosticImagingTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateDiagnosticImagingTypeCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private DiagnosticImagingTypeResponse toResponse(DiagnosticImagingTypeDto dto) {
        return new DiagnosticImagingTypeResponse(dto.id(), dto.name(), dto.description(), dto.createdDate());
    }
}
