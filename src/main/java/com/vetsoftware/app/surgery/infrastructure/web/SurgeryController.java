package com.vetsoftware.app.surgery.infrastructure.web;

import com.vetsoftware.app.surgery.application.command.CreateSurgeryCommand;
import com.vetsoftware.app.surgery.application.command.UpdateSurgeryCommand;
import com.vetsoftware.app.surgery.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.surgery.application.dto.CompanySummaryDto;
import com.vetsoftware.app.surgery.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.dto.SurgeryTypeSummaryDto;
import com.vetsoftware.app.surgery.application.port.in.CreateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.DeleteSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.FindSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesByAnimalUseCase;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesUseCase;
import com.vetsoftware.app.surgery.application.port.in.UpdateSurgeryUseCase;
import com.vetsoftware.app.surgery.infrastructure.web.request.CreateSurgeryRequest;
import com.vetsoftware.app.surgery.infrastructure.web.request.UpdateSurgeryRequest;
import com.vetsoftware.app.surgery.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.surgery.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.surgery.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.surgery.infrastructure.web.response.SurgeryResponse;
import com.vetsoftware.app.surgery.infrastructure.web.response.SurgeryTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/surgeries")
public class SurgeryController {
    private final CreateSurgeryUseCase createUseCase;
    private final UpdateSurgeryUseCase updateUseCase;
    private final FindSurgeryUseCase findUseCase;
    private final ListSurgeriesUseCase listUseCase;
    private final ListSurgeriesByAnimalUseCase listByAnimalUseCase;
    private final DeleteSurgeryUseCase deleteUseCase;

    public SurgeryController(CreateSurgeryUseCase createUseCase,
                             UpdateSurgeryUseCase updateUseCase,
                             FindSurgeryUseCase findUseCase,
                             ListSurgeriesUseCase listUseCase,
                             ListSurgeriesByAnimalUseCase listByAnimalUseCase,
                             DeleteSurgeryUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SurgeryResponse create(@Valid @RequestBody CreateSurgeryRequest request) {
        return toResponse(createUseCase.execute(
            new CreateSurgeryCommand(
                request.date(), request.surgeryTypeId(), request.description(),
                request.medicament(), request.observations(), request.complications(),
                request.animalId(), request.consultationId(), request.companyId())));
    }

    @GetMapping
    public List<SurgeryResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public List<SurgeryResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.listByAnimal(animalId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SurgeryResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SurgeryResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateSurgeryRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateSurgeryCommand(
                id, request.date(), request.surgeryTypeId(), request.description(),
                request.medicament(), request.observations(), request.complications(),
                request.animalId(), request.consultationId(), request.companyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private SurgeryResponse toResponse(SurgeryDto dto) {
        SurgeryTypeSummaryDto st = dto.surgeryType();
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new SurgeryResponse(
            dto.id(), dto.date(),
            new SurgeryTypeSummary(st.id(), st.name()),
            dto.description(), dto.medicament(), dto.observations(), dto.complications(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            co == null ? null : new ConsultationSummary(co.id(), co.date()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate());
    }
}
