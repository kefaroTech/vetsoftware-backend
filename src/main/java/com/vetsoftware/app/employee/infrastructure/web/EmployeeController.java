package com.vetsoftware.app.employee.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.employee.application.command.InviteEmployeeCommand;
import com.vetsoftware.app.employee.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.employee.application.dto.CompanySummaryDto;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import com.vetsoftware.app.employee.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.employee.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.InviteEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesByCompanyUseCase;
import com.vetsoftware.app.employee.application.port.in.ListEmployeesUseCase;
import com.vetsoftware.app.employee.application.port.in.ReactivateEmployeeUseCase;
import com.vetsoftware.app.employee.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.employee.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.employee.infrastructure.web.request.CreateEmployeeRequest;
import com.vetsoftware.app.employee.infrastructure.web.request.UpdateEmployeeRequest;
import com.vetsoftware.app.employee.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.employee.infrastructure.web.response.EmployeeCodeAvailabilityResponse;
import com.vetsoftware.app.employee.infrastructure.web.response.EmployeeCodeSuggestionResponse;
import com.vetsoftware.app.employee.infrastructure.web.response.EmployeeResponse;
import com.vetsoftware.app.employee.infrastructure.web.response.RoleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final InviteEmployeeUseCase inviteUseCase;
    private final UpdateEmployeeUseCase updateUseCase;
    private final FindEmployeeUseCase findUseCase;
    private final ListEmployeesUseCase listUseCase;
    private final ListEmployeesByCompanyUseCase listByCompanyUseCase;
    private final DeleteEmployeeUseCase deleteUseCase;
    private final ReactivateEmployeeUseCase reactivateUseCase;
    private final SuggestEmployeeCodeUseCase suggestCodeUseCase;
    private final CheckEmployeeCodeAvailabilityUseCase checkCodeAvailabilityUseCase;
    private final Authz authz;

    public EmployeeController(InviteEmployeeUseCase inviteUseCase, UpdateEmployeeUseCase updateUseCase,
                               FindEmployeeUseCase findUseCase, ListEmployeesUseCase listUseCase,
                               ListEmployeesByCompanyUseCase listByCompanyUseCase,
                               DeleteEmployeeUseCase deleteUseCase,
                               ReactivateEmployeeUseCase reactivateUseCase,
                               SuggestEmployeeCodeUseCase suggestCodeUseCase,
                               CheckEmployeeCodeAvailabilityUseCase checkCodeAvailabilityUseCase,
                               Authz authz) {
        this.inviteUseCase = inviteUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.suggestCodeUseCase = suggestCodeUseCase;
        this.checkCodeAvailabilityUseCase = checkCodeAvailabilityUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody CreateEmployeeRequest request) {
        return toResponse(inviteUseCase.execute(
            new InviteEmployeeCommand(request.employeeCode(), request.password(), request.name(),
                request.email(), request.companyId(), request.roleIds())
        ));
    }

    @GetMapping
    public List<EmployeeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-company")
    public List<EmployeeResponse> listByCompany() {
        return listByCompanyUseCase.listByCompany(authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    /** Sugiere un código de empleado disponible a partir del nombre (prefijo = iniciales de la empresa). */
    @GetMapping("/suggest-code")
    public EmployeeCodeSuggestionResponse suggestCode(
            @RequestParam(required = false, defaultValue = "") String name) {
        return new EmployeeCodeSuggestionResponse(
            suggestCodeUseCase.suggest(authz.currentCompanyId(), name));
    }

    /** Indica si un código de empleado está libre (chequeo en vivo del formulario). */
    @GetMapping("/code-availability")
    public EmployeeCodeAvailabilityResponse codeAvailability(@RequestParam String code) {
        return new EmployeeCodeAvailabilityResponse(checkCodeAvailabilityUseCase.isAvailable(code));
    }

    @GetMapping("/{id}")
    public EmployeeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateEmployeeCommand(id, request.employeeCode(), request.name(), request.email())
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public EmployeeResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private EmployeeResponse toResponse(EmployeeDto dto) {
        CompanySummaryDto c = dto.company();
        List<RoleSummary> roles = dto.roles().stream()
            .map(r -> new RoleSummary(r.employeeRoleId(), r.id(), r.name(), r.code()))
            .toList();
        return new EmployeeResponse(dto.id(), dto.employeeCode(), dto.name(), dto.email(),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            roles,
            dto.createdDate(),
            dto.enabled(),
            dto.mustChangePassword());
    }
}
