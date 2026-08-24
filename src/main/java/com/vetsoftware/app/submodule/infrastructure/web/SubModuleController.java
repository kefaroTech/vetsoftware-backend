package com.vetsoftware.app.submodule.infrastructure.web;

import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.command.UpdateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.ModuleSummaryDto;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.CreateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.FindSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.ListSubModulesUseCase;
import com.vetsoftware.app.submodule.application.port.in.ReactivateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.UpdateSubModuleUseCase;
import com.vetsoftware.app.submodule.infrastructure.web.request.CreateSubModuleRequest;
import com.vetsoftware.app.submodule.infrastructure.web.request.UpdateSubModuleRequest;
import com.vetsoftware.app.submodule.infrastructure.web.response.ModuleSummary;
import com.vetsoftware.app.submodule.infrastructure.web.response.SubModuleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sub-modules")
public class SubModuleController {
    private final CreateSubModuleUseCase createUseCase;
    private final UpdateSubModuleUseCase updateUseCase;
    private final FindSubModuleUseCase findUseCase;
    private final ListSubModulesUseCase listUseCase;
    private final DeleteSubModuleUseCase deleteUseCase;
    private final ReactivateSubModuleUseCase reactivateUseCase;

    public SubModuleController(CreateSubModuleUseCase createUseCase,
            UpdateSubModuleUseCase updateUseCase, FindSubModuleUseCase findUseCase,
            ListSubModulesUseCase listUseCase, DeleteSubModuleUseCase deleteUseCase,
            ReactivateSubModuleUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubModuleResponse create(@Valid @RequestBody CreateSubModuleRequest request) {
        return toResponse(
                createUseCase.execute(new CreateSubModuleCommand(request.name(), request.code(),
                        request.moduleId(), request.sellable(), request.readOnlyCapable())));
    }

    @GetMapping
    public List<SubModuleResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SubModuleResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SubModuleResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateSubModuleRequest request) {
        return toResponse(
                updateUseCase.execute(new UpdateSubModuleCommand(id, request.name(), request.code(),
                        request.moduleId(), request.sellable(), request.readOnlyCapable())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public SubModuleResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private SubModuleResponse toResponse(SubModuleDto dto) {
        ModuleSummaryDto m = dto.module();
        return new SubModuleResponse(dto.id(), dto.name(), dto.code(),
                new ModuleSummary(m.id(), m.name(), m.code()), dto.sellable(),
                dto.readOnlyCapable(), dto.createdDate(), dto.enabled());
    }
}
