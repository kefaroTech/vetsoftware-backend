package com.vetsoftware.app.animal.infrastructure.web;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.AnimalDto;
import com.vetsoftware.app.animal.application.dto.BreedSummaryDto;
import com.vetsoftware.app.animal.application.dto.CompanySummaryDto;
import com.vetsoftware.app.animal.application.dto.OwnerSummaryDto;
import com.vetsoftware.app.animal.application.dto.SpecieSummaryDto;
import com.vetsoftware.app.animal.application.port.in.CreateAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.DeleteAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.FindAnimalUseCase;
import com.vetsoftware.app.animal.application.port.in.ListAnimalsUseCase;
import com.vetsoftware.app.animal.application.port.in.UpdateAnimalUseCase;
import com.vetsoftware.app.animal.infrastructure.web.request.CreateAnimalRequest;
import com.vetsoftware.app.animal.infrastructure.web.request.UpdateAnimalRequest;
import com.vetsoftware.app.animal.infrastructure.web.response.AnimalResponse;
import com.vetsoftware.app.animal.infrastructure.web.response.BreedSummary;
import com.vetsoftware.app.animal.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.animal.infrastructure.web.response.OwnerSummary;
import com.vetsoftware.app.animal.infrastructure.web.response.SpecieSummary;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/animals")
public class AnimalController {
    private final CreateAnimalUseCase createUseCase;
    private final UpdateAnimalUseCase updateUseCase;
    private final FindAnimalUseCase findUseCase;
    private final ListAnimalsUseCase listUseCase;
    private final DeleteAnimalUseCase deleteUseCase;

    public AnimalController(CreateAnimalUseCase createUseCase, UpdateAnimalUseCase updateUseCase,
                            FindAnimalUseCase findUseCase, ListAnimalsUseCase listUseCase,
                            DeleteAnimalUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalResponse create(@Valid @RequestBody CreateAnimalRequest request,
                                 @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateAnimalCommand(
                request.name(), request.code(), request.specieId(), request.breedId(),
                request.ownerId(), request.gender(), request.weightType(), request.animalType(),
                request.reproductiveState(), request.color(), request.bod(),
                request.weight(), request.size(), request.deceased(), request.deceasedDate(),
                request.companyId()),
            authContext));
    }

    @GetMapping
    public List<AnimalResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AnimalResponse findById(@PathVariable Long id,
                                   @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public AnimalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAnimalRequest request,
                                 @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateAnimalCommand(
                id, request.name(), request.code(), request.specieId(), request.breedId(),
                request.ownerId(), request.gender(), request.weightType(), request.animalType(),
                request.reproductiveState(), request.color(), request.bod(),
                request.weight(), request.size(), request.deceased(), request.deceasedDate(),
                request.companyId()),
            authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private AnimalResponse toResponse(AnimalDto dto) {
        SpecieSummaryDto s = dto.specie();
        BreedSummaryDto b = dto.breed();
        OwnerSummaryDto o = dto.owner();
        CompanySummaryDto c = dto.company();
        return new AnimalResponse(
            dto.id(), dto.name(), dto.code(),
            new SpecieSummary(s.id(), s.name()),
            new BreedSummary(b.id(), b.name()),
            new OwnerSummary(o.id(), o.name(), o.document()),
            dto.gender(), dto.weightType(), dto.animalType(),
            dto.reproductiveState(), dto.color(), dto.bod(),
            dto.weight(), dto.size(), dto.deceased(), dto.deceasedDate(),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate()
        );
    }
}
