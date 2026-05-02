package com.vetsoftware.app.systempermission.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systempermission.application.command.CreateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.command.UpdateSystemPermissionCommand;
import com.vetsoftware.app.systempermission.application.dto.SystemPermissionDto;
import com.vetsoftware.app.systempermission.application.port.in.CreateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.DeleteSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.FindSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.application.port.in.ListSystemPermissionsUseCase;
import com.vetsoftware.app.systempermission.application.port.in.UpdateSystemPermissionUseCase;
import com.vetsoftware.app.systempermission.infrastructure.web.request.CreateSystemPermissionRequest;
import com.vetsoftware.app.systempermission.infrastructure.web.request.UpdateSystemPermissionRequest;
import com.vetsoftware.app.systempermission.infrastructure.web.response.SystemPermissionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system-permissions")
public class SystemPermissionController {
    private final CreateSystemPermissionUseCase createUseCase;
    private final UpdateSystemPermissionUseCase updateUseCase;
    private final FindSystemPermissionUseCase findUseCase;
    private final ListSystemPermissionsUseCase listUseCase;
    private final DeleteSystemPermissionUseCase deleteUseCase;

    public SystemPermissionController(CreateSystemPermissionUseCase createUseCase,
                                      UpdateSystemPermissionUseCase updateUseCase,
                                      FindSystemPermissionUseCase findUseCase,
                                      ListSystemPermissionsUseCase listUseCase,
                                      DeleteSystemPermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemPermissionResponse create(@Valid @RequestBody CreateSystemPermissionRequest request,
                                           @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateSystemPermissionCommand(request.name(), request.code()), authContext));
    }

    @GetMapping
    public List<SystemPermissionResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SystemPermissionResponse findById(@PathVariable Long id,
                                             @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public SystemPermissionResponse update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateSystemPermissionRequest request,
                                           @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateSystemPermissionCommand(id, request.name(), request.code()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private SystemPermissionResponse toResponse(SystemPermissionDto dto) {
        return new SystemPermissionResponse(dto.id(), dto.name(), dto.code(), dto.createdDate());
    }
}
