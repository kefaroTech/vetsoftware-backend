package com.vetsoftware.app.surgerytype.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.surgerytype.application.command.CreateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.command.UpdateSurgeryTypeCommand;
import com.vetsoftware.app.surgerytype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.CreateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.DeleteSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.FindSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ListAvailableSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ListSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.ReactivateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.in.UpdateSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.infrastructure.web.request.CreateSurgeryTypeRequest;
import com.vetsoftware.app.surgerytype.infrastructure.web.request.UpdateSurgeryTypeRequest;
import com.vetsoftware.app.surgerytype.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.surgerytype.infrastructure.web.response.SurgeryTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/surgery-types")
public class SurgeryTypeController {
  private final CreateSurgeryTypeUseCase createUseCase;
  private final UpdateSurgeryTypeUseCase updateUseCase;
  private final FindSurgeryTypeUseCase findUseCase;
  private final ListSurgeryTypesUseCase listUseCase;
  private final ListAvailableSurgeryTypesUseCase listAvailableUseCase;
  private final DeleteSurgeryTypeUseCase deleteUseCase;
  private final ReactivateSurgeryTypeUseCase reactivateUseCase;
  private final Authz authz;

  public SurgeryTypeController(
      CreateSurgeryTypeUseCase createUseCase,
      UpdateSurgeryTypeUseCase updateUseCase,
      FindSurgeryTypeUseCase findUseCase,
      ListSurgeryTypesUseCase listUseCase,
      ListAvailableSurgeryTypesUseCase listAvailableUseCase,
      DeleteSurgeryTypeUseCase deleteUseCase,
      ReactivateSurgeryTypeUseCase reactivateUseCase,
      Authz authz) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.listAvailableUseCase = listAvailableUseCase;
    this.deleteUseCase = deleteUseCase;
    this.reactivateUseCase = reactivateUseCase;
    this.authz = authz;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SurgeryTypeResponse create(@Valid @RequestBody CreateSurgeryTypeRequest request) {
    return toResponse(
        createUseCase.execute(
            new CreateSurgeryTypeCommand(
                request.name(),
                request.description(),
                authz.currentCompanyId(),
                request.general())));
  }

  @GetMapping
  public List<SurgeryTypeResponse> listAll() {
    return listUseCase.listAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/available")
  public List<SurgeryTypeResponse> listAvailable() {
    return listAvailableUseCase.listAvailable(authz.currentCompanyId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @GetMapping("/{id}")
  public SurgeryTypeResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
  }

  @PutMapping("/{id}")
  public SurgeryTypeResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateSurgeryTypeRequest request) {
    return toResponse(
        updateUseCase.execute(
            new UpdateSurgeryTypeCommand(
                id,
                request.name(),
                request.description(),
                authz.currentCompanyId(),
                request.general())));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id);
  }

  @PatchMapping("/{id}/enable")
  public SurgeryTypeResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id));
  }

  private SurgeryTypeResponse toResponse(SurgeryTypeDto dto) {
    CompanySummaryDto c = dto.company();
    return new SurgeryTypeResponse(
        dto.id(),
        dto.name(),
        dto.description(),
        c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
        dto.general(),
        dto.createdDate(),
        dto.enabled());
  }
}
