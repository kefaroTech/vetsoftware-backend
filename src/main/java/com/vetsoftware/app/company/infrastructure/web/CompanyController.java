package com.vetsoftware.app.company.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.CreateCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.infrastructure.web.request.CreateCompanyRequest;
import com.vetsoftware.app.company.infrastructure.web.request.UpdateCompanyRequest;
import com.vetsoftware.app.company.infrastructure.web.response.CompanyResponse;
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
    public CompanyResponse create(@RequestBody CreateCompanyRequest request,
                                  @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateCompanyCommand(request.name(), request.identifier(), request.address(), request.contactNumber()),
            authContext
        ));
    }

    @GetMapping
    public List<CompanyResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id, @RequestBody UpdateCompanyRequest request,
                                  @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateCompanyCommand(id, request.name(), request.identifier(), request.address(), request.contactNumber()),
            authContext
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private CompanyResponse toResponse(CompanyDto dto) {
        return new CompanyResponse(dto.id(), dto.name(), dto.identifier(), dto.address(),
            dto.contactNumber(), dto.createdDate());
    }
}
