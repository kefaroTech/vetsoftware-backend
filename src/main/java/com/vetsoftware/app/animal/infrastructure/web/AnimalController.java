package com.vetsoftware.app.animal.infrastructure.web;

import com.vetsoftware.app.animal.application.command.CreateAnimalCommand;
import com.vetsoftware.app.animal.application.command.UpdateAnimalCommand;
import com.vetsoftware.app.animal.application.dto.*;
import com.vetsoftware.app.animal.application.port.in.*;
import com.vetsoftware.app.animal.infrastructure.web.request.CreateAnimalRequest;
import com.vetsoftware.app.animal.infrastructure.web.request.UpdateAnimalRequest;
import com.vetsoftware.app.animal.infrastructure.web.response.*;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/animals")
public class AnimalController {
    private final CreateAnimalUseCase createUseCase;
    private final UpdateAnimalUseCase updateUseCase;
    private final FindAnimalUseCase findUseCase;
    private final ListAnimalsUseCase listUseCase;
    private final ListAnimalsByOwnerUseCase listByOwnerUseCase;
    private final DeleteAnimalUseCase deleteUseCase;
    private final ReactivateAnimalUseCase reactivateUseCase;
    private final Authz authz;

    public AnimalController(CreateAnimalUseCase createUseCase, UpdateAnimalUseCase updateUseCase,
                            FindAnimalUseCase findUseCase, ListAnimalsUseCase listUseCase,
                            ListAnimalsByOwnerUseCase listByOwnerUseCase,
                            DeleteAnimalUseCase deleteUseCase,
                            ReactivateAnimalUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByOwnerUseCase = listByOwnerUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalResponse create(@Valid @RequestBody CreateAnimalRequest request) {
        return toResponse(createUseCase.execute(
                new CreateAnimalCommand(
                        request.name(), request.code(), request.specieId(), request.breedId(),
                        request.ownerId(), request.gender(), request.weightType(), request.animalType(),
                        request.reproductiveState(), request.colorId(), request.bod(),
                        request.weight(), request.size(), request.deceased(), request.deceasedDate(),
                        authz.currentCompanyId())));
    }

    @GetMapping
    public List<AnimalResponse> listAll() {
        return listUseCase.listAll(authz.currentCompanyId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-owner/{ownerId}")
    public List<AnimalResponse> listByOwner(@PathVariable Long ownerId) {
        return listByOwnerUseCase.listByOwner(ownerId, authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AnimalResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public AnimalResponse update(@PathVariable Long id, @Valid @RequestBody UpdateAnimalRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateAnimalCommand(
                        id, request.name(), request.code(), request.specieId(), request.breedId(),
                        request.ownerId(), request.gender(), request.weightType(), request.animalType(),
                        request.reproductiveState(), request.colorId(), request.bod(),
                        request.weight(), request.size(), request.deceased(), request.deceasedDate(),
                        authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public AnimalResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    private AnimalResponse toResponse(AnimalDto dto) {
        SpecieSummaryDto s = dto.specie();
        BreedSummaryDto b = dto.breed();
        OwnerSummaryDto o = dto.owner();
        CompanySummaryDto c = dto.company();
        AnimalColorSummaryDto co = dto.color();
        return new AnimalResponse(
                dto.id(), dto.name(), dto.code(),
                new SpecieSummary(s.id(), s.name()),
                new BreedSummary(b.id(), b.name()),
                new OwnerSummary(o.id(), o.name(), o.document()),
                dto.gender(), dto.weightType(), dto.animalType(),
                dto.reproductiveState(),
                new AnimalColorSummary(co.id(), co.name()),
                dto.bod(),
                dto.weight(), dto.weightMeasuredAt(), dto.size(), dto.deceased(), dto.deceasedDate(),
                new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.createdDate(), dto.enabled()
        );
    }
}
