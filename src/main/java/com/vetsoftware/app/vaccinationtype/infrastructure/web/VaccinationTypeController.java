package com.vetsoftware.app.vaccinationtype.infrastructure.web;

import com.vetsoftware.app.vaccinationtype.application.command.CreateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.command.UpdateVaccinationTypeCommand;
import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.CreateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.FindVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.ListVaccinationTypesUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.in.UpdateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.request.CreateVaccinationTypeRequest;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.request.UpdateVaccinationTypeRequest;
import com.vetsoftware.app.vaccinationtype.infrastructure.web.response.VaccinationTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vaccination-types")
public class VaccinationTypeController {
    private final CreateVaccinationTypeUseCase createUseCase;
    private final UpdateVaccinationTypeUseCase updateUseCase;
    private final FindVaccinationTypeUseCase findUseCase;
    private final ListVaccinationTypesUseCase listUseCase;
    private final DeleteVaccinationTypeUseCase deleteUseCase;

    public VaccinationTypeController(CreateVaccinationTypeUseCase createUseCase,
                                     UpdateVaccinationTypeUseCase updateUseCase,
                                     FindVaccinationTypeUseCase findUseCase,
                                     ListVaccinationTypesUseCase listUseCase,
                                     DeleteVaccinationTypeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VaccinationTypeResponse create(@Valid @RequestBody CreateVaccinationTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateVaccinationTypeCommand(request.name(), request.description())));
    }

    @GetMapping
    public List<VaccinationTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VaccinationTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public VaccinationTypeResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateVaccinationTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateVaccinationTypeCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private VaccinationTypeResponse toResponse(VaccinationTypeDto dto) {
        return new VaccinationTypeResponse(dto.id(), dto.name(), dto.description(), dto.createdDate());
    }
}
