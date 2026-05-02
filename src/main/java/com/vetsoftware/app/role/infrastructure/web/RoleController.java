package com.vetsoftware.app.role.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.role.application.command.CreateRoleCommand;
import com.vetsoftware.app.role.application.command.UpdateRoleCommand;
import com.vetsoftware.app.role.application.dto.CompanySummaryDto;
import com.vetsoftware.app.role.application.dto.RoleDto;
import com.vetsoftware.app.role.application.port.in.CreateRoleUseCase;
import com.vetsoftware.app.role.application.port.in.DeleteRoleUseCase;
import com.vetsoftware.app.role.application.port.in.FindRoleUseCase;
import com.vetsoftware.app.role.application.port.in.ListRolesUseCase;
import com.vetsoftware.app.role.application.port.in.UpdateRoleUseCase;
import com.vetsoftware.app.role.infrastructure.web.request.CreateRoleRequest;
import com.vetsoftware.app.role.infrastructure.web.request.UpdateRoleRequest;
import com.vetsoftware.app.role.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.role.infrastructure.web.response.RoleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/roles")
public class RoleController {
    private final CreateRoleUseCase createUseCase;
    private final UpdateRoleUseCase updateUseCase;
    private final FindRoleUseCase findUseCase;
    private final ListRolesUseCase listUseCase;
    private final DeleteRoleUseCase deleteUseCase;

    public RoleController(CreateRoleUseCase createUseCase, UpdateRoleUseCase updateUseCase,
                          FindRoleUseCase findUseCase, ListRolesUseCase listUseCase,
                          DeleteRoleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest request,
                               @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateRoleCommand(request.name(), request.code(), request.companyId()), authContext));
    }

    @GetMapping
    public List<RoleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public RoleResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public RoleResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request,
                               @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateRoleCommand(id, request.name(), request.code(), request.companyId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private RoleResponse toResponse(RoleDto dto) {
        CompanySummaryDto c = dto.company();
        return new RoleResponse(
            dto.id(), dto.name(), dto.code(),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate()
        );
    }
}
