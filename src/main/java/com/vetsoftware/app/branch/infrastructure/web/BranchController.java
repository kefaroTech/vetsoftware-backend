package com.vetsoftware.app.branch.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.branch.application.command.CreateBranchCommand;
import com.vetsoftware.app.branch.application.command.UpdateBranchCommand;
import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.dto.CitySummaryDto;
import com.vetsoftware.app.branch.application.dto.CompanySummaryDto;
import com.vetsoftware.app.branch.application.port.in.ActivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.CreateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.DeactivateBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.FindBranchUseCase;
import com.vetsoftware.app.branch.application.port.in.ListBranchesUseCase;
import com.vetsoftware.app.branch.application.port.in.UpdateBranchUseCase;
import com.vetsoftware.app.branch.infrastructure.web.request.CreateBranchRequest;
import com.vetsoftware.app.branch.infrastructure.web.request.UpdateBranchRequest;
import com.vetsoftware.app.branch.infrastructure.web.response.BranchResponse;
import com.vetsoftware.app.branch.infrastructure.web.response.CitySummary;
import com.vetsoftware.app.branch.infrastructure.web.response.CompanySummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/branches")
public class BranchController {
  private final CreateBranchUseCase createUseCase;
  private final UpdateBranchUseCase updateUseCase;
  private final FindBranchUseCase findUseCase;
  private final ListBranchesUseCase listUseCase;
  private final ActivateBranchUseCase activateUseCase;
  private final DeactivateBranchUseCase deactivateUseCase;
  private final Authz authz;

  public BranchController(
      CreateBranchUseCase createUseCase,
      UpdateBranchUseCase updateUseCase,
      FindBranchUseCase findUseCase,
      ListBranchesUseCase listUseCase,
      ActivateBranchUseCase activateUseCase,
      DeactivateBranchUseCase deactivateUseCase,
      Authz authz) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.activateUseCase = activateUseCase;
    this.deactivateUseCase = deactivateUseCase;
    this.authz = authz;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BranchResponse create(@Valid @RequestBody CreateBranchRequest request) {
    return toResponse(
        createUseCase.execute(
            new CreateBranchCommand(
                request.name(),
                request.code(),
                request.address(),
                request.phone(),
                request.cityId(),
                authz.currentCompanyId())));
  }

  @GetMapping
  public List<BranchResponse> listAll() {
    return listUseCase.listAll(authz.currentCompanyId()).stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public BranchResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
  }

  @PutMapping("/{id}")
  public BranchResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateBranchRequest request) {
    return toResponse(
        updateUseCase.execute(
            new UpdateBranchCommand(
                id,
                request.name(),
                request.code(),
                request.address(),
                request.phone(),
                request.cityId(),
                authz.currentCompanyId())));
  }

  @PatchMapping("/{id}/activate")
  public BranchResponse activate(@PathVariable Long id) {
    return toResponse(activateUseCase.execute(id, authz.currentCompanyId()));
  }

  @PatchMapping("/{id}/deactivate")
  public BranchResponse deactivate(@PathVariable Long id) {
    return toResponse(deactivateUseCase.execute(id, authz.currentCompanyId()));
  }

  private BranchResponse toResponse(BranchDto dto) {
    CitySummaryDto c = dto.city();
    CompanySummaryDto co = dto.company();
    return new BranchResponse(
        dto.id(),
        dto.name(),
        dto.code(),
        dto.address(),
        dto.phone(),
        new CitySummary(c.id(), c.name()),
        new CompanySummary(co.id(), co.name(), co.identifier()),
        dto.createdDate(),
        dto.active());
  }
}
