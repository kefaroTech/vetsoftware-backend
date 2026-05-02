package com.vetsoftware.app.state.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.state.application.command.CreateStateCommand;
import com.vetsoftware.app.state.application.command.UpdateStateCommand;
import com.vetsoftware.app.state.application.dto.CountrySummaryDto;
import com.vetsoftware.app.state.application.dto.StateDto;
import com.vetsoftware.app.state.application.port.in.CreateStateUseCase;
import com.vetsoftware.app.state.application.port.in.DeleteStateUseCase;
import com.vetsoftware.app.state.application.port.in.FindStateUseCase;
import com.vetsoftware.app.state.application.port.in.ListStatesUseCase;
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
@RequestMapping("/states")
public class StateController {
    private final CreateStateUseCase createUseCase;
    private final UpdateStateUseCase updateUseCase;
    private final FindStateUseCase findUseCase;
    private final ListStatesUseCase listUseCase;
    private final DeleteStateUseCase deleteUseCase;

    public StateController(CreateStateUseCase createUseCase, UpdateStateUseCase updateUseCase,
                           FindStateUseCase findUseCase, ListStatesUseCase listUseCase,
                           DeleteStateUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StateResponse create(@Valid @RequestBody CreateStateRequest request,
                                @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateStateCommand(request.name(), request.countryId()), authContext));
    }

    @GetMapping
    public List<StateResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public StateResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public StateResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStateRequest request,
                                @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateStateCommand(id, request.name(), request.countryId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private StateResponse toResponse(StateDto dto) {
        CountrySummaryDto c = dto.country();
        return new StateResponse(
            dto.id(),
            dto.name(),
            new CountrySummary(c.id(), c.name()),
            dto.createdDate()
        );
    }
}
