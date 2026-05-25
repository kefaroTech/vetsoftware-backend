package com.vetsoftware.app.state.infrastructure.web;

import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.CountrySummaryDto;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.CreateStateUseCase;
import com.vetsoftware.app.state.application.port.in.DeleteStateUseCase;
import com.vetsoftware.app.state.application.port.in.FindStateUseCase;
import com.vetsoftware.app.state.application.port.in.ListStatesByCountryUseCase;
import com.vetsoftware.app.state.application.port.in.ListStatesUseCase;
import com.vetsoftware.app.state.application.port.in.ReactivateStateUseCase;
import com.vetsoftware.app.state.application.port.in.UpdateStateUseCase;
import com.vetsoftware.app.state.infrastructure.web.request.CreateStateRequest;
import com.vetsoftware.app.state.infrastructure.web.request.UpdateStateRequest;
import com.vetsoftware.app.state.infrastructure.web.response.CountrySummary;
import com.vetsoftware.app.state.infrastructure.web.response.StateResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class StateController {

    private final CreateStateUseCase createUseCase;
    private final UpdateStateUseCase updateUseCase;
    private final FindStateUseCase findUseCase;
    private final ListStatesUseCase listUseCase;
    private final ListStatesByCountryUseCase listByCountryUseCase;
    private final DeleteStateUseCase deleteUseCase;
    private final ReactivateStateUseCase reactivateUseCase;

    public StateController(CreateStateUseCase createUseCase, UpdateStateUseCase updateUseCase,
                           FindStateUseCase findUseCase, ListStatesUseCase listUseCase,
                           ListStatesByCountryUseCase listByCountryUseCase,
                           DeleteStateUseCase deleteUseCase,
                           ReactivateStateUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByCountryUseCase = listByCountryUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping("/states")
    @ResponseStatus(HttpStatus.CREATED)
    public StateResponse create(@Valid @RequestBody CreateStateRequest request) {
        return toResponse(createUseCase.execute(
            new CreateStateCommand(request.name(), request.countryId())));
    }

    @GetMapping("/states")
    public List<StateResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/countries/{countryId}/states")
    public List<StateResponse> listByCountry(@PathVariable Long countryId) {
        return listByCountryUseCase.listByCountry(countryId).stream()
            .map(this::toResponse).toList();
    }

    @GetMapping("/states/{id}")
    public StateResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/states/{id}")
    public StateResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStateRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateStateCommand(id, request.name(), request.countryId())));
    }

    @DeleteMapping("/states/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/states/{id}/enable")
    public StateResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private StateResponse toResponse(StateDto dto) {
        CountrySummaryDto c = dto.country();
        return new StateResponse(
            dto.id(),
            dto.name(),
            new CountrySummary(c.id(), c.name()),
            dto.createdDate(),
            dto.enabled()
        );
    }
}
