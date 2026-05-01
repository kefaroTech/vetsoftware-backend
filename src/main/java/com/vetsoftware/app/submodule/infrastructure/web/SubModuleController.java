package com.vetsoftware.app.submodule.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.command.CreateSubModuleCommand;
import com.vetsoftware.app.submodule.application.command.UpdateSubModuleCommand;
import com.vetsoftware.app.submodule.application.dto.SubModuleDto;
import com.vetsoftware.app.submodule.application.port.in.CreateSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.FindSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.in.ListSubModulesUseCase;
import com.vetsoftware.app.submodule.application.port.in.UpdateSubModuleUseCase;
import com.vetsoftware.app.submodule.infrastructure.web.request.CreateSubModuleRequest;
import com.vetsoftware.app.submodule.infrastructure.web.request.UpdateSubModuleRequest;
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

    public SubModuleController(CreateSubModuleUseCase createUseCase, UpdateSubModuleUseCase updateUseCase,
                               FindSubModuleUseCase findUseCase, ListSubModulesUseCase listUseCase,
                               DeleteSubModuleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubModuleResponse create(@Valid @RequestBody CreateSubModuleRequest request,
                                    @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateSubModuleCommand(request.name(), request.code(), request.moduleId()), authContext));
    }

    @GetMapping
    public List<SubModuleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SubModuleResponse findById(@PathVariable Long id,
                                      @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public SubModuleResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSubModuleRequest request,
                                    @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateSubModuleCommand(id, request.name(), request.code(), request.moduleId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private SubModuleResponse toResponse(SubModuleDto dto) {
        return new SubModuleResponse(dto.id(), dto.name(), dto.code(), dto.moduleId(), dto.createdDate());
    }
}
