package com.vetsoftware.app.deworming.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.deworming.application.command.CreateDewormingCommand;
import com.vetsoftware.app.deworming.application.command.UpdateDewormingCommand;
import com.vetsoftware.app.deworming.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.deworming.application.dto.CompanySummaryDto;
import com.vetsoftware.app.deworming.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.CreateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.DeleteDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.FindDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsByAnimalUseCase;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsUseCase;
import com.vetsoftware.app.deworming.application.port.in.ReactivateDewormingUseCase;
import com.vetsoftware.app.deworming.application.port.in.UpdateDewormingUseCase;
import com.vetsoftware.app.deworming.infrastructure.web.request.CreateDewormingRequest;
import com.vetsoftware.app.deworming.infrastructure.web.request.UpdateDewormingRequest;
import com.vetsoftware.app.deworming.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.deworming.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.deworming.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.deworming.infrastructure.web.response.DewormingResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dewormings")
public class DewormingController {
    private final CreateDewormingUseCase createUseCase;
    private final UpdateDewormingUseCase updateUseCase;
    private final FindDewormingUseCase findUseCase;
    private final ListDewormingsUseCase listUseCase;
    private final ListDewormingsByAnimalUseCase listByAnimalUseCase;
    private final DeleteDewormingUseCase deleteUseCase;
    private final ReactivateDewormingUseCase reactivateUseCase;
    private final Authz authz;

    public DewormingController(CreateDewormingUseCase createUseCase,
            UpdateDewormingUseCase updateUseCase, FindDewormingUseCase findUseCase,
            ListDewormingsUseCase listUseCase, ListDewormingsByAnimalUseCase listByAnimalUseCase,
            DeleteDewormingUseCase deleteUseCase, ReactivateDewormingUseCase reactivateUseCase,
            Authz authz) {
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
    public DewormingResponse create(@Valid @RequestBody CreateDewormingRequest request) {
        return toResponse(createUseCase.execute(new CreateDewormingCommand(request.date(),
                request.lastDeworming(), request.type(), request.product(), request.dosage(),
                request.nextControl(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<DewormingResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public List<DewormingResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.listByAnimal(animalId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DewormingResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public DewormingResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateDewormingRequest request) {
        return toResponse(updateUseCase.execute(new UpdateDewormingCommand(id, request.date(),
                request.lastDeworming(), request.type(), request.product(), request.dosage(),
                request.nextControl(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public DewormingResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private DewormingResponse toResponse(DewormingDto dto) {
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new DewormingResponse(dto.id(), dto.date(), dto.lastDeworming(), dto.type(),
                dto.product(), dto.dosage(), dto.nextControl(), dto.observations(),
                new AnimalSummary(a.id(), a.name(), a.code()),
                co == null ? null : new ConsultationSummary(co.id(), co.date()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled());
    }
}
