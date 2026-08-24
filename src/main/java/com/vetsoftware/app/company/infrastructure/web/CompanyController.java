package com.vetsoftware.app.company.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.ProvisionCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.SearchCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.UpdateCompanyUseCase;
import com.vetsoftware.app.company.infrastructure.web.request.CreateCompanyRequest;
import com.vetsoftware.app.company.infrastructure.web.request.UpdateCompanyRequest;
import com.vetsoftware.app.company.infrastructure.web.response.CitySummary;
import com.vetsoftware.app.company.infrastructure.web.response.CompanyResponse;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/companies")
public class CompanyController {
    private final ProvisionCompanyUseCase provisionUseCase;
    private final UpdateCompanyUseCase updateUseCase;
    private final FindCompanyUseCase findUseCase;
    private final ListCompaniesUseCase listUseCase;
    private final SearchCompaniesUseCase searchUseCase;
    private final DeleteCompanyUseCase deleteUseCase;
    private final Authz authz;

    public CompanyController(ProvisionCompanyUseCase provisionUseCase,
            UpdateCompanyUseCase updateUseCase, FindCompanyUseCase findUseCase,
            ListCompaniesUseCase listUseCase, SearchCompaniesUseCase searchUseCase,
            DeleteCompanyUseCase deleteUseCase, Authz authz) {
        this.provisionUseCase = provisionUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest request) {
        return toResponse(provisionUseCase
                .execute(new CreateCompanyCommand(request.name(), request.identifier(),
                        request.address(), request.contactNumber(), request.cityId())));
    }

    /**
     * El alcance lo pone el servidor, nunca el cliente:
     * {@code currentCompanyIdOrNull()} devuelve la empresa del empleado —y entonces
     * sale una sola fila, la suya— o {@code null} para un principal de plataforma,
     * que es el único que ve el registro completo.
     *
     * <p>
     * VUE-06: devuelve una página, no el censo. {@code page} y {@code pageSize} son
     * opcionales y llevan los mismos valores por defecto que el resto de listados
     * ya paginados del sistema.
     */
    @GetMapping
    public PageResponse<CompanyResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listUseCase.listAll(authz.currentCompanyIdOrNull(), page, pageSize),
                this::toResponse);
    }

    /**
     * Búsqueda por nombre o identificador fiscal, con el mismo alcance derivado del
     * principal que el listado: la consola de plataforma busca en todo el registro,
     * un empleado solo puede encontrar su propia empresa.
     */
    @GetMapping("/search")
    public PageResponse<CompanyResponse> search(@RequestParam("q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                searchUseCase.search(authz.currentCompanyIdOrNull(), query, page, pageSize),
                this::toResponse);
    }

    @GetMapping("/{id}")
    public CompanyResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public CompanyResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateCompanyRequest request) {
        return toResponse(updateUseCase
                .execute(new UpdateCompanyCommand(id, request.name(), request.identifier(),
                        request.address(), request.contactNumber(), request.cityId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private CompanyResponse toResponse(CompanyDto dto) {
        CitySummaryDto c = dto.city();
        return new CompanyResponse(dto.id(), dto.name(), dto.identifier(), dto.address(),
                dto.contactNumber(), new CitySummary(c.id(), c.name()), dto.createdDate(),
                dto.enabled());
    }
}
