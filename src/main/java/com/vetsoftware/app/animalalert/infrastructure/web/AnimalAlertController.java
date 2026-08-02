package com.vetsoftware.app.animalalert.infrastructure.web;

import com.vetsoftware.app.animalalert.application.command.CreateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.command.UpdateAnimalAlertCommand;
import com.vetsoftware.app.animalalert.application.dto.AnimalAlertDto;
import com.vetsoftware.app.animalalert.application.port.in.CreateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.in.DeleteAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.port.in.ListAnimalAlertsByAnimalUseCase;
import com.vetsoftware.app.animalalert.application.port.in.UpdateAnimalAlertUseCase;
import com.vetsoftware.app.animalalert.application.query.ListAnimalAlertsByAnimalQuery;
import com.vetsoftware.app.animalalert.infrastructure.web.request.CreateAnimalAlertRequest;
import com.vetsoftware.app.animalalert.infrastructure.web.request.UpdateAnimalAlertRequest;
import com.vetsoftware.app.animalalert.infrastructure.web.response.AnimalAlertResponse;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/animal-alerts")
public class AnimalAlertController {
    private final CreateAnimalAlertUseCase createUseCase;
    private final UpdateAnimalAlertUseCase updateUseCase;
    private final DeleteAnimalAlertUseCase deleteUseCase;
    private final ListAnimalAlertsByAnimalUseCase listByAnimalUseCase;
    private final Authz authz;

    public AnimalAlertController(CreateAnimalAlertUseCase createUseCase,
            UpdateAnimalAlertUseCase updateUseCase, DeleteAnimalAlertUseCase deleteUseCase,
            ListAnimalAlertsByAnimalUseCase listByAnimalUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalAlertResponse create(@Valid @RequestBody CreateAnimalAlertRequest request) {
        return toResponse(createUseCase
                .execute(new CreateAnimalAlertCommand(request.animalId(), request.type(),
                        request.description(), request.severity(), authz.currentCompanyId())));
    }

    @GetMapping("/by-animal/{animalId}")
    public List<AnimalAlertResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase
                .execute(new ListAnimalAlertsByAnimalQuery(animalId, authz.currentCompanyId()))
                .stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}")
    public AnimalAlertResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateAnimalAlertRequest request) {
        return toResponse(updateUseCase.execute(new UpdateAnimalAlertCommand(id, request.type(),
                request.description(), request.severity(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private AnimalAlertResponse toResponse(AnimalAlertDto dto) {
        return new AnimalAlertResponse(dto.id(), dto.animalId(), dto.animalName(), dto.type(),
                dto.description(), dto.severity(), dto.createdDate(), dto.enabled());
    }
}
