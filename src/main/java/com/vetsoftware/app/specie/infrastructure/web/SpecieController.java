package com.vetsoftware.app.specie.infrastructure.web;

import com.vetsoftware.app.specie.application.command.CreateSpecieCommand;
import com.vetsoftware.app.specie.application.command.UpdateSpecieCommand;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.CreateSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.DeleteSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.FindSpecieUseCase;
import com.vetsoftware.app.specie.application.port.in.ListSpeciesUseCase;
import com.vetsoftware.app.specie.application.port.in.UpdateSpecieUseCase;
import com.vetsoftware.app.specie.infrastructure.web.request.CreateSpecieRequest;
import com.vetsoftware.app.specie.infrastructure.web.request.UpdateSpecieRequest;
import com.vetsoftware.app.specie.infrastructure.web.response.SpecieResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/species")
public class SpecieController {
    private final CreateSpecieUseCase createUseCase;
    private final UpdateSpecieUseCase updateUseCase;
    private final FindSpecieUseCase findUseCase;
    private final ListSpeciesUseCase listUseCase;
    private final DeleteSpecieUseCase deleteUseCase;

    public SpecieController(CreateSpecieUseCase createUseCase, UpdateSpecieUseCase updateUseCase,
                            FindSpecieUseCase findUseCase, ListSpeciesUseCase listUseCase,
                            DeleteSpecieUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecieResponse create(@Valid @RequestBody CreateSpecieRequest request) {
        return toResponse(createUseCase.execute(new CreateSpecieCommand(request.name())));
    }

    @GetMapping
    public List<SpecieResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SpecieResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SpecieResponse update(@PathVariable Long id, @Valid @RequestBody UpdateSpecieRequest request) {
        return toResponse(updateUseCase.execute(new UpdateSpecieCommand(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private SpecieResponse toResponse(SpecieDto dto) {
        return new SpecieResponse(dto.id(), dto.name(), dto.createdDate());
    }
}
