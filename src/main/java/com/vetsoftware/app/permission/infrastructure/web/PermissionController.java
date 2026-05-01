package com.vetsoftware.app.permission.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.FindPermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsUseCase;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.infrastructure.web.request.CreatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.request.UpdatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.response.PermissionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/permissions")
public class PermissionController {
    private final CreatePermissionUseCase createUseCase;
    private final UpdatePermissionUseCase updateUseCase;
    private final FindPermissionUseCase findUseCase;
    private final ListPermissionsUseCase listUseCase;
    private final DeletePermissionUseCase deleteUseCase;

    public PermissionController(CreatePermissionUseCase createUseCase,
                                 UpdatePermissionUseCase updateUseCase,
                                 FindPermissionUseCase findUseCase,
                                 ListPermissionsUseCase listUseCase,
                                 DeletePermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@RequestBody CreatePermissionRequest request,
                                      @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreatePermissionCommand(request.name(), request.code(), request.companyId(), request.subModuleId()),
            authContext));
    }

    @GetMapping
    public List<PermissionResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PermissionResponse findById(@PathVariable Long id,
                                        @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public PermissionResponse update(@PathVariable Long id,
                                      @RequestBody UpdatePermissionRequest request,
                                      @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdatePermissionCommand(id, request.name(), request.code(), request.companyId(), request.subModuleId()),
            authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private PermissionResponse toResponse(PermissionDto dto) {
        return new PermissionResponse(dto.id(), dto.name(), dto.code(), dto.companyId(), dto.subModuleId(), dto.createdDate());
    }
}
