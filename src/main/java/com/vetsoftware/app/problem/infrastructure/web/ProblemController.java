package com.vetsoftware.app.problem.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.problem.application.command.CreateProblemCommand;
import com.vetsoftware.app.problem.application.command.UpdateProblemCommand;
import com.vetsoftware.app.problem.application.dto.ProblemDto;
import com.vetsoftware.app.problem.application.port.in.CreateProblemUseCase;
import com.vetsoftware.app.problem.application.port.in.DeleteProblemUseCase;
import com.vetsoftware.app.problem.application.port.in.ListProblemsByAnimalUseCase;
import com.vetsoftware.app.problem.application.port.in.UpdateProblemUseCase;
import com.vetsoftware.app.problem.application.query.ListProblemsByAnimalQuery;
import com.vetsoftware.app.problem.infrastructure.web.request.CreateProblemRequest;
import com.vetsoftware.app.problem.infrastructure.web.request.UpdateProblemRequest;
import com.vetsoftware.app.problem.infrastructure.web.response.ProblemResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problems")
public class ProblemController {
    private final CreateProblemUseCase createUseCase;
    private final UpdateProblemUseCase updateUseCase;
    private final DeleteProblemUseCase deleteUseCase;
    private final ListProblemsByAnimalUseCase listByAnimalUseCase;
    private final Authz authz;

    public ProblemController(CreateProblemUseCase createUseCase,
                             UpdateProblemUseCase updateUseCase,
                             DeleteProblemUseCase deleteUseCase,
                             ListProblemsByAnimalUseCase listByAnimalUseCase,
                             Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProblemResponse create(@Valid @RequestBody CreateProblemRequest request) {
        return toResponse(createUseCase.execute(
            new CreateProblemCommand(
                request.animalId(), request.description(), request.status(),
                request.onsetDate(), request.resolvedDate(), request.notes(),
                authz.currentCompanyId())));
    }

    @GetMapping("/by-animal/{animalId}")
    public List<ProblemResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.execute(
                new ListProblemsByAnimalQuery(animalId, authz.currentCompanyId()))
            .stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public ProblemResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateProblemRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateProblemCommand(
                id, request.description(), request.status(), request.onsetDate(),
                request.resolvedDate(), request.notes(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private ProblemResponse toResponse(ProblemDto dto) {
        return new ProblemResponse(
            dto.id(), dto.animalId(), dto.animalName(), dto.description(),
            dto.status(), dto.onsetDate(), dto.resolvedDate(), dto.notes(),
            dto.createdDate(), dto.enabled());
    }
}
