package com.vetsoftware.app.breed.infrastructure.web;

import com.vetsoftware.app.breed.application.command.CreateBreedCommand;
import com.vetsoftware.app.breed.application.command.UpdateBreedCommand;
import com.vetsoftware.app.breed.application.dto.BreedDto;
import com.vetsoftware.app.breed.application.dto.SpecieSummaryDto;
import com.vetsoftware.app.breed.application.port.in.CreateBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.DeleteBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.FindBreedUseCase;
import com.vetsoftware.app.breed.application.port.in.ListBreedsUseCase;
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
@RequestMapping("/breeds")
public class BreedController {
    private final CreateBreedUseCase createUseCase;
    private final UpdateBreedUseCase updateUseCase;
    private final FindBreedUseCase findUseCase;
    private final ListBreedsUseCase listUseCase;
    private final DeleteBreedUseCase deleteUseCase;

    public BreedController(CreateBreedUseCase createUseCase, UpdateBreedUseCase updateUseCase,
                           FindBreedUseCase findUseCase, ListBreedsUseCase listUseCase,
                           DeleteBreedUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BreedResponse create(@Valid @RequestBody CreateBreedRequest request) {
        return toResponse(createUseCase.execute(
            new CreateBreedCommand(request.name(), request.specieId())));
    }

    @GetMapping
    public List<BreedResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BreedResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public BreedResponse update(@PathVariable Long id, @Valid @RequestBody UpdateBreedRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateBreedCommand(id, request.name(), request.specieId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private BreedResponse toResponse(BreedDto dto) {
        SpecieSummaryDto s = dto.specie();
        return new BreedResponse(
            dto.id(), dto.name(),
            new SpecieSummary(s.id(), s.name()),
            dto.createdDate()
        );
    }
}
