package com.vetsoftware.app.breed.infrastructure.web;

import com.vetsoftware.app.breed.application.command.CreateBreedCommand;
import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.dto.SpecieSummaryDto;
import com.vetsoftware.app.breed.application.port.in.CreateBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.DeleteBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.FindBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.ListBreedsBySpecieUseCase;
import com.vetsoftware.app.breed.application.port.in.ListBreedsUseCase;
import com.vetsoftware.app.breed.application.port.in.ReactivateBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.UpdateBreedUseCase;
import com.vetsoftware.app.breed.infrastructure.web.request.CreateBreedRequest;
import com.vetsoftware.app.breed.infrastructure.web.request.UpdateBreedRequest;
import com.vetsoftware.app.breed.infrastructure.web.response.BreedResponse;
import com.vetsoftware.app.breed.infrastructure.web.response.SpecieSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class BreedController {
  private final CreateBreedUseCase createUseCase;
  private final UpdateBreedUseCase updateUseCase;
  private final FindBreedUseCase findUseCase;
  private final ListBreedsUseCase listUseCase;
  private final ListBreedsBySpecieUseCase listBySpecieUseCase;
  private final DeleteBreedUseCase deleteUseCase;
  private final ReactivateBreedUseCase reactivateUseCase;

  public BreedController(
      CreateBreedUseCase createUseCase,
      UpdateBreedUseCase updateUseCase,
      FindBreedUseCase findUseCase,
      ListBreedsUseCase listUseCase,
      ListBreedsBySpecieUseCase listBySpecieUseCase,
      DeleteBreedUseCase deleteUseCase,
      ReactivateBreedUseCase reactivateUseCase) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.listBySpecieUseCase = listBySpecieUseCase;
    this.deleteUseCase = deleteUseCase;
    this.reactivateUseCase = reactivateUseCase;
  }

  @PostMapping("/breeds")
  @ResponseStatus(HttpStatus.CREATED)
  public BreedResponse create(@Valid @RequestBody CreateBreedRequest request) {
    return toResponse(
        createUseCase.execute(new CreateBreedCommand(request.name(), request.specieId())));
  }

  @GetMapping("/breeds")
  public List<BreedResponse> listAll() {
    return listUseCase.listAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/species/{specieId}/breeds")
  public List<BreedResponse> listBySpecie(@PathVariable Long specieId) {
    return listBySpecieUseCase.listBySpecie(specieId).stream().map(this::toResponse).toList();
  }

  @GetMapping("/breeds/{id}")
  public BreedResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id));
  }

  @PutMapping("/breeds/{id}")
  public BreedResponse update(
      @PathVariable Long id, @Valid @RequestBody UpdateBreedRequest request) {
    return toResponse(
        updateUseCase.execute(new UpdateBreedCommand(id, request.name(), request.specieId())));
  }

  @DeleteMapping("/breeds/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id);
  }

  @PatchMapping("/breeds/{id}/enable")
  public BreedResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id));
  }

  private BreedResponse toResponse(BreedDto dto) {
    SpecieSummaryDto s = dto.specie();
    return new BreedResponse(
        dto.id(),
        dto.name(),
        new SpecieSummary(s.id(), s.name()),
        dto.createdDate(),
        dto.enabled());
  }
}
