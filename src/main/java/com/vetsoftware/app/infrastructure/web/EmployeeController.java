package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.application.command.CreateEmployeeCommand;
import com.vetsoftware.app.application.command.UpdateEmployeeCommand;
import com.vetsoftware.app.application.dto.EmployeeDto;
import com.vetsoftware.app.application.port.in.CreateEmployeeUseCase;
import com.vetsoftware.app.application.port.in.DeleteEmployeeUseCase;
import com.vetsoftware.app.application.port.in.FindEmployeeUseCase;
import com.vetsoftware.app.application.port.in.ListEmployeesUseCase;
import com.vetsoftware.app.application.port.in.UpdateEmployeeUseCase;
import com.vetsoftware.app.infrastructure.web.request.CreateEmployeeRequest;
import com.vetsoftware.app.infrastructure.web.request.UpdateEmployeeRequest;
import com.vetsoftware.app.infrastructure.web.response.EmployeeResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    private final CreateEmployeeUseCase createUseCase;
    private final UpdateEmployeeUseCase updateUseCase;
    private final FindEmployeeUseCase findUseCase;
    private final ListEmployeesUseCase listUseCase;
    private final DeleteEmployeeUseCase deleteUseCase;

    public EmployeeController(CreateEmployeeUseCase createUseCase, UpdateEmployeeUseCase updateUseCase,
                               FindEmployeeUseCase findUseCase, ListEmployeesUseCase listUseCase,
                               DeleteEmployeeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody CreateEmployeeRequest request) {
        return toResponse(createUseCase.execute(
            new CreateEmployeeCommand(request.employeeCode(), request.password(), request.name(),
                request.email(), request.status(), request.companyId(), request.createdBy())
        ));
    }

    @GetMapping
    public List<EmployeeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EmployeeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @RequestBody UpdateEmployeeRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateEmployeeCommand(id, request.employeeCode(), request.name(), request.email(), request.status())
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private EmployeeResponse toResponse(EmployeeDto dto) {
        return new EmployeeResponse(dto.id(), dto.employeeCode(), dto.name(), dto.email(),
            dto.status(), dto.companyId(), dto.createdDate(), dto.createdBy());
    }
}
