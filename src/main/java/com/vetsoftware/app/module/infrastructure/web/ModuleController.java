package com.vetsoftware.app.module.infrastructure.web;

import com.vetsoftware.app.module.application.command.CreateModuleCommand;
import com.vetsoftware.app.module.application.command.UpdateModuleCommand;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.CreateModuleUseCase;
import com.vetsoftware.app.module.application.port.in.DeleteModuleUseCase;
import com.vetsoftware.app.module.application.port.in.FindModuleUseCase;
import com.vetsoftware.app.module.application.port.in.ListModulesUseCase;
import com.vetsoftware.app.module.application.port.in.ReactivateModuleUseCase;
import com.vetsoftware.app.module.application.port.in.UpdateModuleUseCase;
import com.vetsoftware.app.module.infrastructure.web.request.CreateModuleRequest;
import com.vetsoftware.app.module.infrastructure.web.request.UpdateModuleRequest;
import com.vetsoftware.app.module.infrastructure.web.response.ModuleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/modules")
public class ModuleController {
  private final CreateModuleUseCase createUseCase;
  private final UpdateModuleUseCase updateUseCase;
  private final FindModuleUseCase findUseCase;
  private final ListModulesUseCase listUseCase;
  private final DeleteModuleUseCase deleteUseCase;
  private final ReactivateModuleUseCase reactivateUseCase;

  public ModuleController(
      CreateModuleUseCase createUseCase,
      UpdateModuleUseCase updateUseCase,
      FindModuleUseCase findUseCase,
      ListModulesUseCase listUseCase,
      DeleteModuleUseCase deleteUseCase,
      ReactivateModuleUseCase reactivateUseCase) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.deleteUseCase = deleteUseCase;
    this.reactivateUseCase = reactivateUseCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ModuleResponse create(@Valid @RequestBody CreateModuleRequest request) {
    return toResponse(
        createUseCase.execute(new CreateModuleCommand(request.name(), request.code())));
  }

  @GetMapping
  public List<ModuleResponse> listAll() {
    return listUseCase.listAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/{id}")
  public ModuleResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id));
  }

  @PutMapping("/{id}")
  public ModuleResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateModuleRequest request) {
    return toResponse(
        updateUseCase.execute(new UpdateModuleCommand(id, request.name(), request.code())));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id);
  }

  @PatchMapping("/{id}/enable")
  public ModuleResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id));
  }

  private ModuleResponse toResponse(ModuleDto dto) {
    return new ModuleResponse(dto.id(), dto.name(), dto.code(), dto.createdDate(), dto.enabled());
  }
}
