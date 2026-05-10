package com.vetsoftware.app.spatype.infrastructure.web;

import com.vetsoftware.app.spatype.application.command.CreateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.command.UpdateSpaTypeCommand;
import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.CreateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.DeleteSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.FindSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.in.ListSpaTypesUseCase;
import com.vetsoftware.app.spatype.application.port.in.UpdateSpaTypeUseCase;
import com.vetsoftware.app.spatype.infrastructure.web.request.CreateSpaTypeRequest;
import com.vetsoftware.app.spatype.infrastructure.web.request.UpdateSpaTypeRequest;
import com.vetsoftware.app.spatype.infrastructure.web.response.SpaTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spa-types")
public class SpaTypeController {
    private final CreateSpaTypeUseCase createUseCase;
    private final UpdateSpaTypeUseCase updateUseCase;
    private final FindSpaTypeUseCase findUseCase;
    private final ListSpaTypesUseCase listUseCase;
    private final DeleteSpaTypeUseCase deleteUseCase;

    public SpaTypeController(CreateSpaTypeUseCase createUseCase,
                             UpdateSpaTypeUseCase updateUseCase,
                             FindSpaTypeUseCase findUseCase,
                             ListSpaTypesUseCase listUseCase,
                             DeleteSpaTypeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaTypeResponse create(@Valid @RequestBody CreateSpaTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateSpaTypeCommand(request.name(), request.description())));
    }

    @GetMapping
    public List<SpaTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SpaTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SpaTypeResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateSpaTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateSpaTypeCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private SpaTypeResponse toResponse(SpaTypeDto dto) {
        return new SpaTypeResponse(dto.id(), dto.name(), dto.description(), dto.createdDate());
    }
}
