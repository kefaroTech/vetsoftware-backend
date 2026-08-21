package com.vetsoftware.app.rolepermission.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.dto.RoleSummaryDto;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.FindRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.SyncRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.CreateRolePermissionRequest;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.SyncRolePermissionsRequest;
import com.vetsoftware.app.rolepermission.infrastructure.web.response.PermissionSummary;
import com.vetsoftware.app.rolepermission.infrastructure.web.response.RolePermissionResponse;
import com.vetsoftware.app.rolepermission.infrastructure.web.response.RoleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/role-permissions")
public class RolePermissionController {
    private final CreateRolePermissionUseCase createUseCase;
    private final SyncRolePermissionsUseCase syncUseCase;
    private final FindRolePermissionUseCase findUseCase;
    private final ListRolePermissionsUseCase listUseCase;
    private final DeleteRolePermissionUseCase deleteUseCase;
    private final Authz authz;

    public RolePermissionController(CreateRolePermissionUseCase createUseCase,
            SyncRolePermissionsUseCase syncUseCase, FindRolePermissionUseCase findUseCase,
            ListRolePermissionsUseCase listUseCase, DeleteRolePermissionUseCase deleteUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.syncUseCase = syncUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponse create(@Valid @RequestBody CreateRolePermissionRequest request) {
        return toResponse(createUseCase.execute(new CreateRolePermissionCommand(request.roleId(),
                request.permissionId(), authz.currentCompanyIdOrNull())));
    }

    @PutMapping("/by-role/{roleId}")
    public List<RolePermissionResponse> syncByRole(@PathVariable Long roleId,
            @Valid @RequestBody SyncRolePermissionsRequest request) {
        return syncUseCase.execute(new SyncRolePermissionsCommand(roleId, request.permissionIds(),
                authz.currentCompanyIdOrNull())).stream().map(this::toResponse).toList();
    }

    @GetMapping
    public List<RolePermissionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RolePermissionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    private RolePermissionResponse toResponse(RolePermissionDto dto) {
        RoleSummaryDto r = dto.role();
        PermissionSummaryDto p = dto.permission();
        return new RolePermissionResponse(dto.id(), new RoleSummary(r.id(), r.name(), r.code()),
                new PermissionSummary(p.id(), p.name(), p.code()), dto.createdDate(),
                dto.enabled());
    }
}
