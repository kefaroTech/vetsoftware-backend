package com.vetsoftware.app.city.infrastructure.web;

import com.vetsoftware.app.city.application.command.CreateCityCommand;
import com.vetsoftware.app.city.application.command.UpdateCityCommand;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.dto.StateSummaryDto;
import com.vetsoftware.app.city.application.port.in.CreateCityUseCase;
import com.vetsoftware.app.city.application.port.in.DeleteCityUseCase;
import com.vetsoftware.app.city.application.port.in.FindCityUseCase;
import com.vetsoftware.app.city.application.port.in.ListCitiesByStateUseCase;
import com.vetsoftware.app.city.application.port.in.ListCitiesUseCase;
import com.vetsoftware.app.city.application.port.in.ReactivateCityUseCase;
import com.vetsoftware.app.city.application.port.in.UpdateCityUseCase;
import com.vetsoftware.app.city.infrastructure.web.request.CreateCityRequest;
import com.vetsoftware.app.city.infrastructure.web.request.UpdateCityRequest;
import com.vetsoftware.app.city.infrastructure.web.response.CityResponse;
import com.vetsoftware.app.city.infrastructure.web.response.StateSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CityController {

  private final CreateCityUseCase createUseCase;
  private final UpdateCityUseCase updateUseCase;
  private final FindCityUseCase findUseCase;
  private final ListCitiesUseCase listUseCase;
  private final ListCitiesByStateUseCase listByStateUseCase;
  private final DeleteCityUseCase deleteUseCase;
  private final ReactivateCityUseCase reactivateUseCase;

  public CityController(
      CreateCityUseCase createUseCase,
      UpdateCityUseCase updateUseCase,
      FindCityUseCase findUseCase,
      ListCitiesUseCase listUseCase,
      ListCitiesByStateUseCase listByStateUseCase,
      DeleteCityUseCase deleteUseCase,
      ReactivateCityUseCase reactivateUseCase) {
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.findUseCase = findUseCase;
    this.listUseCase = listUseCase;
    this.listByStateUseCase = listByStateUseCase;
    this.deleteUseCase = deleteUseCase;
    this.reactivateUseCase = reactivateUseCase;
  }

  @PostMapping("/cities")
  @ResponseStatus(HttpStatus.CREATED)
  public CityResponse create(@Valid @RequestBody CreateCityRequest request) {
    return toResponse(
        createUseCase.execute(
            new CreateCityCommand(request.name(), request.stateId(), request.daneCode())));
  }

  @GetMapping("/cities")
  public List<CityResponse> listAll() {
    return listUseCase.listAll().stream().map(this::toResponse).toList();
  }

  @GetMapping("/states/{stateId}/cities")
  public List<CityResponse> listByState(@PathVariable Long stateId) {
    return listByStateUseCase.listByState(stateId).stream().map(this::toResponse).toList();
  }

  @GetMapping("/cities/{id}")
  public CityResponse findById(@PathVariable Long id) {
    return toResponse(findUseCase.findById(id));
  }

  @PutMapping("/cities/{id}")
  public CityResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCityRequest request) {
    return toResponse(
        updateUseCase.execute(
            new UpdateCityCommand(id, request.name(), request.stateId(), request.daneCode())));
  }

  @DeleteMapping("/cities/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.execute(id);
  }

  @PatchMapping("/cities/{id}/enable")
  public CityResponse enable(@PathVariable Long id) {
    return toResponse(reactivateUseCase.execute(id));
  }

  private CityResponse toResponse(CityDto dto) {
    StateSummaryDto s = dto.state();
    return new CityResponse(
        dto.id(),
        dto.name(),
        new StateSummary(s.id(), s.name()),
        dto.daneCode(),
        dto.createdDate(),
        dto.enabled());
  }
}
