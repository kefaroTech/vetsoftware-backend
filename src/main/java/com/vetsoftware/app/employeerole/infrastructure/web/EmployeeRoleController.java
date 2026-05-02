package com.vetsoftware.app.employeerole.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employeerole.application.command.CreateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.command.UpdateEmployeeRoleCommand;
import com.vetsoftware.app.employeerole.application.dto.EmployeeRoleDto;
import com.vetsoftware.app.employeerole.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.employeerole.application.dto.RoleSummaryDto;
import com.vetsoftware.app.employeerole.application.port.in.CreateEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.DeleteEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.FindEmployeeRoleUseCase;
import com.vetsoftware.app.employeerole.application.port.in.ListEmployeeRolesUseCase;
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

    public EmployeeRoleController(CreateEmployeeRoleUseCase createUseCase,
                                  UpdateEmployeeRoleUseCase updateUseCase,
                                  FindEmployeeRoleUseCase findUseCase,
                                  ListEmployeeRolesUseCase listUseCase,
                                  DeleteEmployeeRoleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeRoleResponse create(@Valid @RequestBody CreateEmployeeRoleRequest request,
                                       @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateEmployeeRoleCommand(request.employeeId(), request.roleId()), authContext));
    }

    @GetMapping
    public List<EmployeeRoleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmployeeRoleResponse findById(@PathVariable Long id,
                                         @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public EmployeeRoleResponse update(@PathVariable Long id,
                                       @Valid @RequestBody UpdateEmployeeRoleRequest request,
                                       @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateEmployeeRoleCommand(id, request.employeeId(), request.roleId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private EmployeeRoleResponse toResponse(EmployeeRoleDto dto) {
        EmployeeSummaryDto e = dto.employee();
        RoleSummaryDto r = dto.role();
        return new EmployeeRoleResponse(
            dto.id(),
            new EmployeeSummary(e.id(), e.employeeCode(), e.name()),
            new RoleSummary(r.id(), r.name(), r.code()),
            dto.createdDate()
        );
    }
}
