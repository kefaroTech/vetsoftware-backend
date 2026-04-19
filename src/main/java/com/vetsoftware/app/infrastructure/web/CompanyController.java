package com.vetsoftware.app.infrastructure.web;

import com.vetsoftware.app.application.command.CreateCompanyCommand;
import com.vetsoftware.app.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.infrastructure.web.request.CreateCompanyRequest;
import com.vetsoftware.app.infrastructure.web.request.UpdateCompanyRequest;
import com.vetsoftware.app.infrastructure.web.response.CompanyResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private final CreateCompanyUseCase createUseCase;
    private final UpdateCompanyUseCase updateUseCase;
    private final FindCompanyUseCase findUseCase;
    private final ListCompaniesUseCase listUseCase;
    private final DeleteCompanyUseCase deleteUseCase;

    public CompanyController(CreateCompanyUseCase createUseCase, UpdateCompanyUseCase updateUseCase,
                             FindCompanyUseCase findUseCase, ListCompaniesUseCase listUseCase,
                             DeleteCompanyUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@RequestBody CreateCompanyRequest request) {
        return toResponse(createUseCase.execute(
            new CreateCompanyCommand(request.name(), request.identifier(), request.address(), request.contactNumber(), request.createdBy())
        ));
    }

    @GetMapping
    public List<CompanyResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @RequestBody UpdateCompanyRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateCompanyCommand(id, request.name(), request.identifier(), request.address(), request.contactNumber())
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private CompanyResponse toResponse(CompanyDto dto) {
        return new CompanyResponse(dto.id(), dto.name(), dto.identifier(), dto.address(),
            dto.contactNumber(), dto.createdDate(), dto.createdBy());
    }
}
