package com.vetsoftware.app.economicactivity.infrastructure.web;

import com.vetsoftware.app.economicactivity.application.command.CreateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.command.UpdateEconomicActivityCommand;
import com.vetsoftware.app.economicactivity.application.dto.EconomicActivityDto;
import com.vetsoftware.app.economicactivity.application.port.in.CreateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.DeleteEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.FindEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.ListEconomicActivitiesUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.ReactivateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.application.port.in.UpdateEconomicActivityUseCase;
import com.vetsoftware.app.economicactivity.infrastructure.web.request.CreateEconomicActivityRequest;
import com.vetsoftware.app.economicactivity.infrastructure.web.request.UpdateEconomicActivityRequest;
import com.vetsoftware.app.economicactivity.infrastructure.web.response.EconomicActivityResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/economic-activities")
public class EconomicActivityController {
  private final CreateEconomicActivityUseCase createUseCase;
  private final UpdateEconomicActivityUseCase updateUseCase;
  private final FindEconomicActivityUseCase findUseCase;
  private final ListEconomicActivitiesUseCase listUseCase;
  private final DeleteEconomicActivityUseCase deleteUseCase;
  private final ReactivateEconomicActivityUseCase reactivateUseCase;

  public EconomicActivityController(
      CreateEconomicActivityUseCase createUseCase,
      UpdateEconomicActivityUseCase updateUseCase,
      FindEconomicActivityUseCase findUseCase,
      ListEconomicActivitiesUseCase listUseCase,
      DeleteEconomicActivityUseCase deleteUseCase,
      ReactivateEconomicActivityUseCase reactivateUseCase) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.deleteUseCase = deleteUseCase;
    this.reactivateUseCase = reactivateUseCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EconomicActivityResponse create(
      @Valid @RequestBody CreateEconomicActivityRequest request) {
    return toResponse(
        createUseCase.execute(new CreateEconomicActivityCommand(request.code(), request.name())));
  }

  @GetMapping
  public List<EconomicActivityResponse> listAll() {
    return listUseCase.listAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public EconomicActivityResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id));
  }

  @PutMapping("/{id}")
  public EconomicActivityResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateEconomicActivityRequest request) {
    return toResponse(
        updateUseCase.execute(
            new UpdateEconomicActivityCommand(id, request.code(), request.name())));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id);
  }

  @PatchMapping("/{id}/enable")
  public EconomicActivityResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id));
  }

  private EconomicActivityResponse toResponse(EconomicActivityDto dto) {
    return new EconomicActivityResponse(
        dto.id(), dto.code(), dto.name(), dto.createdDate(), dto.enabled());
  }
}
