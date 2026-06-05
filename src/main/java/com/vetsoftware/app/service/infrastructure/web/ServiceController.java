package com.vetsoftware.app.service.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.service.application.command.CreateServiceCommand;
import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.command.UpdateServiceCommand;
import com.vetsoftware.app.service.application.dto.CompanySummaryDto;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.dto.ServiceCategorySummaryDto;
import com.vetsoftware.app.service.application.dto.ServiceDto;
import com.vetsoftware.app.service.application.dto.TaxSummaryDto;
import com.vetsoftware.app.service.application.port.in.CreateServiceUseCase;
import com.vetsoftware.app.service.application.port.in.DeleteServiceUseCase;
import com.vetsoftware.app.service.application.port.in.FindServiceUseCase;
import com.vetsoftware.app.service.application.port.in.ListServicesByCompanyUseCase;
import com.vetsoftware.app.service.application.port.in.ReactivateServiceUseCase;
import com.vetsoftware.app.service.application.port.in.SearchServicesUseCase;
import com.vetsoftware.app.service.application.port.in.UpdateServiceUseCase;
import com.vetsoftware.app.service.infrastructure.web.request.CreateServiceRequest;
import com.vetsoftware.app.service.infrastructure.web.request.UpdateServiceRequest;
import com.vetsoftware.app.service.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.service.infrastructure.web.response.ServiceCategorySummary;
import com.vetsoftware.app.service.infrastructure.web.response.ServiceResponse;
import com.vetsoftware.app.service.infrastructure.web.response.TaxSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/services")
public class ServiceController {
    private final CreateServiceUseCase createUseCase;
    private final UpdateServiceUseCase updateUseCase;
    private final FindServiceUseCase findUseCase;
    private final ListServicesByCompanyUseCase listByCompanyUseCase;
    private final SearchServicesUseCase searchUseCase;
    private final DeleteServiceUseCase deleteUseCase;
    private final ReactivateServiceUseCase reactivateUseCase;
    private final Authz authz;

    public ServiceController(CreateServiceUseCase createUseCase,
                             UpdateServiceUseCase updateUseCase,
                             FindServiceUseCase findUseCase,
                             ListServicesByCompanyUseCase listByCompanyUseCase,
                             SearchServicesUseCase searchUseCase,
                             DeleteServiceUseCase deleteUseCase,
                             ReactivateServiceUseCase reactivateUseCase,
                             Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody CreateServiceRequest request) {
        return toResponse(createUseCase.execute(
            new CreateServiceCommand(
                request.name(), request.price(), request.hasTax(), request.notes(),
                request.serviceCategoryId(), request.taxId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<ServiceResponse> listByCompany() {
        return listByCompanyUseCase.listByCompany(authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public PageResponse<ServiceResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long serviceCategoryId,
            @RequestParam(required = false) Long taxId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ServiceDto> result = searchUseCase.execute(new SearchServicesCommand(
            authz.currentCompanyId(), name, serviceCategoryId, taxId, page, pageSize));
        return new PageResponse<>(
            result.content().stream().map(this::toResponse).toList(),
            result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public ServiceResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateServiceRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateServiceCommand(
                id, request.name(), request.price(), request.hasTax(), request.notes(),
                request.serviceCategoryId(), request.taxId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public ServiceResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private ServiceResponse toResponse(ServiceDto dto) {
        ServiceCategorySummaryDto sc = dto.serviceCategory();
        TaxSummaryDto t = dto.tax();
        CompanySummaryDto c = dto.company();
        return new ServiceResponse(
            dto.id(), dto.name(), dto.price(), dto.hasTax(), dto.notes(),
            new ServiceCategorySummary(sc.id(), sc.name()),
            t == null ? null : new TaxSummary(t.id(), t.name(), t.percentage()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate(), dto.enabled());
    }
}
