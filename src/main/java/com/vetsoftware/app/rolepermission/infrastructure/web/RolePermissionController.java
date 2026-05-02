package com.vetsoftware.app.rolepermission.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.rolepermission.application.command.CreateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.command.UpdateRolePermissionCommand;
import com.vetsoftware.app.rolepermission.application.dto.PermissionSummaryDto;
import com.vetsoftware.app.rolepermission.application.dto.RolePermissionDto;
import com.vetsoftware.app.rolepermission.application.dto.RoleSummaryDto;
import com.vetsoftware.app.rolepermission.application.port.in.CreateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.DeleteRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.FindRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.ListRolePermissionsUseCase;
import com.vetsoftware.app.rolepermission.application.port.in.UpdateRolePermissionUseCase;
import com.vetsoftware.app.rolepermission.infrastructure.web.request.CreateRolePermissionRequest;
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
    private final UpdateRolePermissionUseCase updateUseCase;
    private final FindRolePermissionUseCase findUseCase;
    private final ListRolePermissionsUseCase listUseCase;
    private final DeleteRolePermissionUseCase deleteUseCase;

    public RolePermissionController(CreateRolePermissionUseCase createUseCase,
                                    UpdateRolePermissionUseCase updateUseCase,
                                    FindRolePermissionUseCase findUseCase,
                                    ListRolePermissionsUseCase listUseCase,
                                    DeleteRolePermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolePermissionResponse create(@Valid @RequestBody CreateRolePermissionRequest request,
                                         @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateRolePermissionCommand(request.roleId(), request.permissionId()), authContext));
    }

    @GetMapping
    public List<RolePermissionResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RolePermissionResponse findById(@PathVariable Long id,
                                           @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public RolePermissionResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateRolePermissionRequest request,
                                         @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateRolePermissionCommand(id, request.roleId(), request.permissionId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private RolePermissionResponse toResponse(RolePermissionDto dto) {
        RoleSummaryDto r = dto.role();
        PermissionSummaryDto p = dto.permission();
        return new RolePermissionResponse(
            dto.id(),
            new RoleSummary(r.id(), r.name(), r.code()),
            new PermissionSummary(p.id(), p.name(), p.code()),
            dto.createdDate()
        );
    }
}
