package com.vetsoftware.app.employeerole.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.employeerole.application.dto.RoleSummaryDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.FindEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.ListEmployeeRolesUseCase;
import com.vetsoftware.app.employeerole.application.port.in.ReactivateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.UpdateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.infrastructure.web.request.CreateEmployeeRoleRequest;
import com.vetsoftware.app.employeerole.infrastructure.web.request.UpdateEmployeeRoleRequest;
import com.vetsoftware.app.employeerole.infrastructure.web.response.EmployeeRoleResponse;
import com.vetsoftware.app.employeerole.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.employeerole.infrastructure.web.response.RoleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee-roles")
public class EmployeeRoleController {
    private final CreateEmployeeRoleUseCase createUseCase;
    private final UpdateEmployeeRoleUseCase updateUseCase;
    private final FindEmployeeRoleUseCase findUseCase;
    private final ListEmployeeRolesUseCase listUseCase;
    private final DeleteEmployeeRoleUseCase deleteUseCase;
    private final ReactivateEmployeeRoleUseCase reactivateUseCase;
    private final Authz authz;

    public EmployeeRoleController(CreateEmployeeRoleUseCase createUseCase,
            UpdateEmployeeRoleUseCase updateUseCase, FindEmployeeRoleUseCase findUseCase,
            ListEmployeeRolesUseCase listUseCase, DeleteEmployeeRoleUseCase deleteUseCase,
            ReactivateEmployeeRoleUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeRoleResponse create(@Valid @RequestBody CreateEmployeeRoleRequest request) {
        // La empresa la sella el contexto, nunca el request: el cliente elige a quien
        // asignar el rol, no en que empresa. currentCompanyIdOrNull() devuelve null
        // solo
        // para un principal SYSTEM, que si opera global.
        return toResponse(createUseCase.execute(new CreateEmployeeRoleCommand(request.employeeId(),
                request.roleId(), authz.currentCompanyIdOrNull())));
    }

    @GetMapping
    public List<EmployeeRoleResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmployeeRoleResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public EmployeeRoleResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRoleRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateEmployeeRoleCommand(id, request.employeeId(), request.roleId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    @PatchMapping("/{id}/enable")
    public EmployeeRoleResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyIdOrNull()));
    }

    private EmployeeRoleResponse toResponse(EmployeeRoleDto dto) {
        EmployeeSummaryDto e = dto.employee();
        RoleSummaryDto r = dto.role();
        return new EmployeeRoleResponse(dto.id(),
                new EmployeeSummary(e.id(), e.employeeCode(), e.name()),
                new RoleSummary(r.id(), r.name(), r.code()), dto.createdDate(), dto.enabled());
    }
}
