package com.vetsoftware.app.tax.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.tax.application.command.CreateTaxCommand;
import com.vetsoftware.app.tax.application.command.UpdateTaxCommand;
import com.vetsoftware.app.tax.application.dto.CompanySummaryDto;
import com.vetsoftware.app.tax.application.dto.TaxDto;
import com.vetsoftware.app.tax.application.port.in.CreateTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.DeleteTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.FindTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.ListTaxesUseCase;
import com.vetsoftware.app.tax.application.port.in.ReactivateTaxUseCase;
import com.vetsoftware.app.tax.application.port.in.UpdateTaxUseCase;
import com.vetsoftware.app.tax.infrastructure.web.request.CreateTaxRequest;
import com.vetsoftware.app.tax.infrastructure.web.request.UpdateTaxRequest;
import com.vetsoftware.app.tax.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.tax.infrastructure.web.response.TaxResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/taxes")
public class TaxController {
    private final CreateTaxUseCase createUseCase;
    private final UpdateTaxUseCase updateUseCase;
    private final FindTaxUseCase findUseCase;
    private final ListTaxesUseCase listUseCase;
    private final DeleteTaxUseCase deleteUseCase;
    private final ReactivateTaxUseCase reactivateUseCase;
    private final Authz authz;

    public TaxController(CreateTaxUseCase createUseCase, UpdateTaxUseCase updateUseCase,
            FindTaxUseCase findUseCase, ListTaxesUseCase listUseCase,
            DeleteTaxUseCase deleteUseCase, ReactivateTaxUseCase reactivateUseCase, Authz authz) {
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
    public TaxResponse create(@Valid @RequestBody CreateTaxRequest request) {
        return toResponse(createUseCase.execute(new CreateTaxCommand(request.name(),
                request.percentage(), request.taxScheme(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<TaxResponse> listAll() {
        return listUseCase.listByCompany(authz.currentCompanyId()).stream().map(this::toResponse)
                .toList();
    }

    @GetMapping("/disabled")
    public List<TaxResponse> listDisabled() {
        return listUseCase.listDisabledByCompany(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TaxResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public TaxResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTaxRequest request) {
        return toResponse(updateUseCase.execute(new UpdateTaxCommand(id, request.name(),
                request.percentage(), request.taxScheme(), authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull(), request.version())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public TaxResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private TaxResponse toResponse(TaxDto dto) {
        CompanySummaryDto c = dto.company();
        return new TaxResponse(dto.id(), dto.name(), dto.percentage(), dto.taxScheme(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.createdDate(), dto.updatedDate(), dto.updatedBy(), dto.version(),
                dto.enabled());
    }
}
