package com.vetsoftware.app.servicecategory.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.servicecategory.application.command.CreateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.command.UpdateServiceCategoryCommand;
import com.vetsoftware.app.servicecategory.application.dto.CompanySummaryDto;
import com.vetsoftware.app.servicecategory.application.dto.ServiceCategoryDto;
import com.vetsoftware.app.servicecategory.application.port.in.CreateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.DeleteServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.FindServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.ListServiceCategoriesUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.ReactivateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.application.port.in.UpdateServiceCategoryUseCase;
import com.vetsoftware.app.servicecategory.infrastructure.web.request.CreateServiceCategoryRequest;
import com.vetsoftware.app.servicecategory.infrastructure.web.request.UpdateServiceCategoryRequest;
import com.vetsoftware.app.servicecategory.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.servicecategory.infrastructure.web.response.ServiceCategoryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service-categories")
public class ServiceCategoryController {
  private final CreateServiceCategoryUseCase createUseCase;
  private final UpdateServiceCategoryUseCase updateUseCase;
  private final FindServiceCategoryUseCase findUseCase;
  private final ListServiceCategoriesUseCase listUseCase;
  private final DeleteServiceCategoryUseCase deleteUseCase;
  private final ReactivateServiceCategoryUseCase reactivateUseCase;
  private final Authz authz;

  public ServiceCategoryController(
      CreateServiceCategoryUseCase createUseCase,
      UpdateServiceCategoryUseCase updateUseCase,
      FindServiceCategoryUseCase findUseCase,
      ListServiceCategoriesUseCase listUseCase,
      DeleteServiceCategoryUseCase deleteUseCase,
      ReactivateServiceCategoryUseCase reactivateUseCase,
      Authz authz) {
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
  public ServiceCategoryResponse create(@Valid @RequestBody CreateServiceCategoryRequest request) {
    return toResponse(
        createUseCase.execute(
            new CreateServiceCategoryCommand(
                request.name(), request.description(), authz.currentCompanyId())));
  }

  @GetMapping
  public List<ServiceCategoryResponse> listByCompany() {
    return listUseCase.listByCompany(authz.currentCompanyId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public ServiceCategoryResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
  }

  @PutMapping("/{id}")
  public ServiceCategoryResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateServiceCategoryRequest request) {
    return toResponse(
        updateUseCase.execute(
            new UpdateServiceCategoryCommand(
                id,
                request.name(),
                request.description(),
                authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull(),
                request.version())));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id, authz.currentCompanyId());
  }

  @PatchMapping("/{id}/enable")
  public ServiceCategoryResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
  }

  private ServiceCategoryResponse toResponse(ServiceCategoryDto dto) {
    CompanySummaryDto c = dto.company();
    return new ServiceCategoryResponse(
        dto.id(),
        dto.name(),
        dto.description(),
        c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
        dto.createdDate(),
        dto.updatedDate(),
        dto.updatedBy(),
        dto.version(),
        dto.enabled());
  }
}
