package com.vetsoftware.app.animalcolor.infrastructure.web;

import com.vetsoftware.app.animalcolor.application.command.CreateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.command.UpdateAnimalColorCommand;
import com.vetsoftware.app.animalcolor.application.dto.AnimalColorDto;
import com.vetsoftware.app.animalcolor.application.port.in.CreateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.DeleteAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.FindAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.ListAnimalColorsUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.ReactivateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.application.port.in.UpdateAnimalColorUseCase;
import com.vetsoftware.app.animalcolor.infrastructure.web.request.CreateAnimalColorRequest;
import com.vetsoftware.app.animalcolor.infrastructure.web.request.UpdateAnimalColorRequest;
import com.vetsoftware.app.animalcolor.infrastructure.web.response.AnimalColorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/animal-colors")
public class AnimalColorController {
    private final CreateAnimalColorUseCase createUseCase;
    private final UpdateAnimalColorUseCase updateUseCase;
    private final FindAnimalColorUseCase findUseCase;
    private final ListAnimalColorsUseCase listUseCase;
    private final DeleteAnimalColorUseCase deleteUseCase;
    private final ReactivateAnimalColorUseCase reactivateUseCase;

    public AnimalColorController(CreateAnimalColorUseCase createUseCase,
            UpdateAnimalColorUseCase updateUseCase, FindAnimalColorUseCase findUseCase,
            ListAnimalColorsUseCase listUseCase, DeleteAnimalColorUseCase deleteUseCase,
            ReactivateAnimalColorUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AnimalColorResponse create(@Valid @RequestBody CreateAnimalColorRequest request) {
        return toResponse(createUseCase.execute(new CreateAnimalColorCommand(request.name())));
    }

    @GetMapping
    public List<AnimalColorResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AnimalColorResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public AnimalColorResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateAnimalColorRequest request) {
        return toResponse(updateUseCase.execute(new UpdateAnimalColorCommand(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public AnimalColorResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private AnimalColorResponse toResponse(AnimalColorDto dto) {
        return new AnimalColorResponse(dto.id(), dto.name(), dto.createdDate(), dto.enabled());
    }
}
