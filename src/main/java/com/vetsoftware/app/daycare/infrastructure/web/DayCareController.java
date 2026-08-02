package com.vetsoftware.app.daycare.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.daycare.application.command.CreateDayCareCommand;
import com.vetsoftware.app.daycare.application.command.UpdateDayCareCommand;
import com.vetsoftware.app.daycare.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.daycare.application.dto.CompanySummaryDto;
import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.CreateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.DeleteDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.FindDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresByAnimalUseCase;
import com.vetsoftware.app.daycare.application.port.in.ListDayCaresUseCase;
import com.vetsoftware.app.daycare.application.port.in.ReactivateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.in.UpdateDayCareUseCase;
import com.vetsoftware.app.daycare.infrastructure.web.request.CreateDayCareRequest;
import com.vetsoftware.app.daycare.infrastructure.web.request.UpdateDayCareRequest;
import com.vetsoftware.app.daycare.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.daycare.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.daycare.infrastructure.web.response.DayCareResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/daycares")
public class DayCareController {
    private final CreateDayCareUseCase createUseCase;
    private final UpdateDayCareUseCase updateUseCase;
    private final FindDayCareUseCase findUseCase;
    private final ListDayCaresUseCase listUseCase;
    private final ListDayCaresByAnimalUseCase listByAnimalUseCase;
    private final DeleteDayCareUseCase deleteUseCase;
    private final ReactivateDayCareUseCase reactivateUseCase;
    private final Authz authz;

    public DayCareController(CreateDayCareUseCase createUseCase, UpdateDayCareUseCase updateUseCase,
            FindDayCareUseCase findUseCase, ListDayCaresUseCase listUseCase,
            ListDayCaresByAnimalUseCase listByAnimalUseCase, DeleteDayCareUseCase deleteUseCase,
            ReactivateDayCareUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DayCareResponse create(@Valid @RequestBody CreateDayCareRequest request) {
        return toResponse(createUseCase.execute(new CreateDayCareCommand(request.date(),
                request.startDate(), request.endDate(), request.type(), request.objects(),
                request.observations(), request.animalId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<DayCareResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public List<DayCareResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.listByAnimal(animalId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DayCareResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public DayCareResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateDayCareRequest request) {
        return toResponse(updateUseCase.execute(new UpdateDayCareCommand(id, request.date(),
                request.startDate(), request.endDate(), request.type(), request.objects(),
                request.observations(), request.animalId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public DayCareResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private DayCareResponse toResponse(DayCareDto dto) {
        AnimalSummaryDto a = dto.animal();
        CompanySummaryDto c = dto.company();
        return new DayCareResponse(dto.id(), dto.date(), dto.startDate(), dto.endDate(), dto.type(),
                dto.objects(), dto.observations(), new AnimalSummary(a.id(), a.name(), a.code()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled());
    }
}
