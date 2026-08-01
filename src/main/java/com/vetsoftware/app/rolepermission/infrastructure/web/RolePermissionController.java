package com.vetsoftware.app.rolepermission.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.command.SyncRolePermissionsCommand;
import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.dto.RoleSummaryDto;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.FindRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsByCompanyUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ReactivateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.SyncRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.CreateRolePermissionRequest;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.SyncRolePermissionsRequest;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.UpdateRolePermissionRequest;
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
    private final UpdateRolePermissionUseCase updateUseCase;
    private final FindRolePermissionUseCase findUseCase;
    private final ListRolePermissionsUseCase listUseCase;
    private final ListRolePermissionsByCompanyUseCase listByCompanyUseCase;
    private final DeleteRolePermissionUseCase deleteUseCase;
    private final ReactivateRolePermissionUseCase reactivateUseCase;
    private final Authz authz;

    public RolePermissionController(CreateRolePermissionUseCase createUseCase,
                                    SyncRolePermissionsUseCase syncUseCase,
                                    UpdateRolePermissionUseCase updateUseCase,
                                    FindRolePermissionUseCase findUseCase,
                                    ListRolePermissionsUseCase listUseCase,
                                    ListRolePermissionsByCompanyUseCase listByCompanyUseCase,
                                    DeleteRolePermissionUseCase deleteUseCase,
                                    ReactivateRolePermissionUseCase reactivateUseCase,
                                    Authz authz) {
        this.createUseCase = createUseCase;
        this.syncUseCase = syncUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponse create(@Valid @RequestBody CreateRolePermissionRequest request) {
        return toResponse(createUseCase.execute(
            new CreateRolePermissionCommand(
                request.roleId(), request.permissionId(), authz.currentCompanyIdOrNull())));
    }

    @PutMapping("/by-role/{roleId}")
    public List<RolePermissionResponse> syncByRole(@PathVariable Long roleId,
                                                   @Valid @RequestBody SyncRolePermissionsRequest request) {
        return syncUseCase.execute(new SyncRolePermissionsCommand(roleId, request.permissionIds()))
            .stream().map(this::toResponse).toList();
    }

    @GetMapping
    public List<RolePermissionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-company")
    public List<RolePermissionResponse> listByCompany() {
        return listByCompanyUseCase.listByCompany(authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RolePermissionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public RolePermissionResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateRolePermissionRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateRolePermissionCommand(
                id, request.roleId(), request.permissionId(), authz.currentCompanyIdOrNull())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    @PatchMapping("/{id}/enable")
    public RolePermissionResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyIdOrNull()));
    }

    private RolePermissionResponse toResponse(RolePermissionDto dto) {
        RoleSummaryDto r = dto.role();
        PermissionSummaryDto p = dto.permission();
        return new RolePermissionResponse(
            dto.id(),
            new RoleSummary(r.id(), r.name(), r.code()),
            new PermissionSummary(p.id(), p.name(), p.code()),
            dto.createdDate(),
            dto.enabled()
        );
    }
}
