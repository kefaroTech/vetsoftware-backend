package com.vetsoftware.app.company.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CitySummaryDto;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import com.vetsoftware.app.company.application.port.in.DeleteCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.FindCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ListDisabledCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.ListCompaniesUseCase;
import com.vetsoftware.app.company.application.port.in.ProvisionCompanyUseCase;
import com.vetsoftware.app.company.application.port.in.ReactivateCompanyUseCase;
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
    private final ListDisabledCompaniesUseCase listDisabledUseCase;
    private final SearchCompaniesUseCase searchUseCase;
    private final DeleteCompanyUseCase deleteUseCase;
    private final ReactivateCompanyUseCase reactivateUseCase;
    private final Authz authz;

    public CompanyController(ProvisionCompanyUseCase provisionUseCase,
            UpdateCompanyUseCase updateUseCase, FindCompanyUseCase findUseCase,
            ListCompaniesUseCase listUseCase, SearchCompaniesUseCase searchUseCase,
            ListDisabledCompaniesUseCase listDisabledUseCase, DeleteCompanyUseCase deleteUseCase,
            ReactivateCompanyUseCase reactivateUseCase, Authz authz) {
        this.provisionUseCase = provisionUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listDisabledUseCase = listDisabledUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
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

    /**
     * El ARCHIVO de empresas, y la pieza que hacia inalcanzable a
     * {@code PATCH /companies/{id}/enable}: con
     * {@code @SQLRestriction("enabled = true")} sobre la entidad, ni el listado ni
     * la busqueda ni {@code GET /companies/{id}} pueden devolver una empresa dada
     * de baja, asi que hasta ahora restaurarla exigia saberse su id de memoria.
     *
     * <p>
     * Misma forma que los cinco {@code /disabled} ya existentes
     * ({@code /medicaments/disabled}, {@code /products/disabled},
     * {@code /services/disabled}, {@code /taxes/disabled} y
     * {@code /admin/medicaments/disabled}), y mismo alcance derivado del principal
     * que {@link #listAll}: {@code currentCompanyIdOrNull()} da {@code null} para
     * la consola de plataforma —que ve el archivo completo— y la empresa del
     * empleado en cualquier otro caso. El cliente no puede declarar alcance.
     *
     * <p>
     * La ruta literal gana a {@code /{id}} en el emparejamiento de Spring, igual
     * que {@code /search}: {@code /companies/disabled} no se resuelve como
     * {@code findById("disabled")}.
     */
    @GetMapping("/disabled")
    public PageResponse<CompanyResponse> listDisabled(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(
                listDisabledUseCase.listDisabled(authz.currentCompanyIdOrNull(), page, pageSize),
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

    /**
     * Saca del archivo una empresa borrada por error. Mismo verbo y misma forma que
     * el resto de reactivaciones del sistema ({@code PATCH /cities/{id}/enable} y
     * sus veintinueve hermanos), y sin cuerpo: no hay nada que elegir, solo el id
     * de la URL.
     *
     * <p>
     * Hasta que existio este endpoint, deshacer un archivado exigia un
     * {@code UPDATE} a mano en produccion.
     */
    @PatchMapping("/{id}/enable")
    public CompanyResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private CompanyResponse toResponse(CompanyDto dto) {
        CitySummaryDto c = dto.city();
        return new CompanyResponse(dto.id(), dto.name(), dto.identifier(), dto.address(),
                dto.contactNumber(), new CitySummary(c.id(), c.name()), dto.createdDate(),
                dto.enabled());
    }
}
