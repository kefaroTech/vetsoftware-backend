package com.vetsoftware.app.laboratorytest.infrastructure.web;

import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.CompanySummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestTypeSummaryDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ChangeLaboratoryTestStatusUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.CreateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.DeleteLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.FindLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsByAnimalUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.UpdateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.ChangeLaboratoryTestStatusRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.CreateLaboratoryTestRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.UpdateLaboratoryTestRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.LaboratoryTestResponse;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.LaboratoryTestTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratory-tests")
public class LaboratoryTestController {
    private final CreateLaboratoryTestUseCase createUseCase;
    private final UpdateLaboratoryTestUseCase updateUseCase;
    private final ChangeLaboratoryTestStatusUseCase changeStatusUseCase;
    private final FindLaboratoryTestUseCase findUseCase;
    private final ListLaboratoryTestsUseCase listUseCase;
    private final ListLaboratoryTestsByAnimalUseCase listByAnimalUseCase;
    private final DeleteLaboratoryTestUseCase deleteUseCase;

    public LaboratoryTestController(CreateLaboratoryTestUseCase createUseCase,
                                    UpdateLaboratoryTestUseCase updateUseCase,
                                    ChangeLaboratoryTestStatusUseCase changeStatusUseCase,
                                    FindLaboratoryTestUseCase findUseCase,
                                    ListLaboratoryTestsUseCase listUseCase,
                                    ListLaboratoryTestsByAnimalUseCase listByAnimalUseCase,
                                    DeleteLaboratoryTestUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryTestResponse create(@Valid @RequestBody CreateLaboratoryTestRequest request) {
        return toResponse(createUseCase.execute(
            new CreateLaboratoryTestCommand(
                request.date(), request.testTypeId(), request.quantity(),
                request.diagnosis(), request.animalId(), request.consultationId(),
                request.companyId())));
    }

    @GetMapping
    public List<LaboratoryTestResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public List<LaboratoryTestResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.listByAnimal(animalId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public LaboratoryTestResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public LaboratoryTestResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateLaboratoryTestRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateLaboratoryTestCommand(
                id, request.date(), request.testTypeId(), request.quantity(),
                request.diagnosis(), request.animalId(), request.consultationId(),
                request.companyId())));
    }

    @PatchMapping("/{id}/status")
    public LaboratoryTestResponse changeStatus(@PathVariable Long id,
                                               @Valid @RequestBody ChangeLaboratoryTestStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(
            new ChangeLaboratoryTestStatusCommand(id, request.status())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private LaboratoryTestResponse toResponse(LaboratoryTestDto dto) {
        LaboratoryTestTypeSummaryDto tt = dto.testType();
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new LaboratoryTestResponse(
            dto.id(), dto.date(),
            new LaboratoryTestTypeSummary(tt.id(), tt.name()),
            dto.quantity(), dto.diagnosis(), dto.status(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            co == null ? null : new ConsultationSummary(co.id(), co.date()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate());
    }
}
