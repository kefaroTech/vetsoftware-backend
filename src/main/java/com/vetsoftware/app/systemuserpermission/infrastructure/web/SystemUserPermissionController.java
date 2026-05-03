package com.vetsoftware.app.systemuserpermission.infrastructure.web;

import com.vetsoftware.app.systemuserpermission.application.command.CreateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.command.UpdateSystemUserPermissionCommand;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemPermissionSummaryDto;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserPermissionDto;
import com.vetsoftware.app.systemuserpermission.application.dto.SystemUserSummaryDto;
import com.vetsoftware.app.systemuserpermission.application.port.in.CreateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.DeleteSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.FindSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.ListSystemUserPermissionsUseCase;
import com.vetsoftware.app.systemuserpermission.application.port.in.UpdateSystemUserPermissionUseCase;
import com.vetsoftware.app.systemuserpermission.infrastructure.web.request.CreateSystemUserPermissionRequest;
import com.vetsoftware.app.systemuserpermission.infrastructure.web.request.UpdateSystemUserPermissionRequest;
import com.vetsoftware.app.systemuserpermission.infrastructure.web.response.SystemPermissionSummary;
import com.vetsoftware.app.systemuserpermission.infrastructure.web.response.SystemUserPermissionResponse;
import com.vetsoftware.app.systemuserpermission.infrastructure.web.response.SystemUserSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system-user-permissions")
public class SystemUserPermissionController {
    private final CreateSystemUserPermissionUseCase createUseCase;
    private final UpdateSystemUserPermissionUseCase updateUseCase;
    private final FindSystemUserPermissionUseCase findUseCase;
    private final ListSystemUserPermissionsUseCase listUseCase;
    private final DeleteSystemUserPermissionUseCase deleteUseCase;

    public SystemUserPermissionController(CreateSystemUserPermissionUseCase createUseCase,
                                          UpdateSystemUserPermissionUseCase updateUseCase,
                                          FindSystemUserPermissionUseCase findUseCase,
                                          ListSystemUserPermissionsUseCase listUseCase,
                                          DeleteSystemUserPermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemUserPermissionResponse create(@Valid @RequestBody CreateSystemUserPermissionRequest request) {
        return toResponse(createUseCase.execute(
            new CreateSystemUserPermissionCommand(request.systemUserId(), request.systemPermissionId())));
    }

    @GetMapping
    public List<SystemUserPermissionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SystemUserPermissionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SystemUserPermissionResponse update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateSystemUserPermissionRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateSystemUserPermissionCommand(id, request.systemUserId(), request.systemPermissionId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private SystemUserPermissionResponse toResponse(SystemUserPermissionDto dto) {
        SystemUserSummaryDto u = dto.systemUser();
        SystemPermissionSummaryDto p = dto.systemPermission();
        return new SystemUserPermissionResponse(
            dto.id(),
            new SystemUserSummary(u.id(), u.code()),
            new SystemPermissionSummary(p.id(), p.name(), p.code()),
            dto.createdDate()
        );
    }
}
