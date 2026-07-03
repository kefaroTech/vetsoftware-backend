package com.vetsoftware.app.vaccination.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.vaccination.application.command.CreateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.command.UpdateVaccinationCommand;
import com.vetsoftware.app.vaccination.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.vaccination.application.dto.CompanySummaryDto;
import com.vetsoftware.app.vaccination.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.dto.VaccinationTypeSummaryDto;
import com.vetsoftware.app.vaccination.application.port.in.CreateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.DeleteVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.FindVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsByAnimalUseCase;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsUseCase;
import com.vetsoftware.app.vaccination.application.port.in.ReactivateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.in.UpdateVaccinationUseCase;
import com.vetsoftware.app.vaccination.infrastructure.web.request.CreateVaccinationRequest;
import com.vetsoftware.app.vaccination.infrastructure.web.request.UpdateVaccinationRequest;
import com.vetsoftware.app.vaccination.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.vaccination.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.vaccination.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.vaccination.infrastructure.web.response.VaccinationResponse;
import com.vetsoftware.app.vaccination.infrastructure.web.response.VaccinationTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vaccinations")
public class VaccinationController {
    private final CreateVaccinationUseCase createUseCase;
    private final UpdateVaccinationUseCase updateUseCase;
    private final FindVaccinationUseCase findUseCase;
    private final ListVaccinationsUseCase listUseCase;
    private final ListVaccinationsByAnimalUseCase listByAnimalUseCase;
    private final DeleteVaccinationUseCase deleteUseCase;
    private final ReactivateVaccinationUseCase reactivateUseCase;
    private final Authz authz;

    public VaccinationController(CreateVaccinationUseCase createUseCase,
                                 UpdateVaccinationUseCase updateUseCase,
                                 FindVaccinationUseCase findUseCase,
                                 ListVaccinationsUseCase listUseCase,
                                 ListVaccinationsByAnimalUseCase listByAnimalUseCase,
                                 DeleteVaccinationUseCase deleteUseCase,
                                 ReactivateVaccinationUseCase reactivateUseCase,
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
    public VaccinationResponse create(@Valid @RequestBody CreateVaccinationRequest request) {
        return toResponse(createUseCase.execute(
            new CreateVaccinationCommand(
                request.date(), request.vaccinationTypeId(), request.lot(),
                request.notes(), request.route(), request.applicationSite(), request.nextVaccination(),
                request.animalId(), request.consultationId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<VaccinationResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public List<VaccinationResponse> listByAnimal(@PathVariable Long animalId) {
        return listByAnimalUseCase.listByAnimal(animalId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public VaccinationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public VaccinationResponse update(@PathVariable Long id,
                                      @Valid @RequestBody UpdateVaccinationRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateVaccinationCommand(
                id, request.date(), request.vaccinationTypeId(), request.lot(),
                request.notes(), request.route(), request.applicationSite(), request.nextVaccination(),
                request.animalId(), request.consultationId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public VaccinationResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private VaccinationResponse toResponse(VaccinationDto dto) {
        VaccinationTypeSummaryDto vt = dto.vaccinationType();
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new VaccinationResponse(
            dto.id(), dto.date(),
            new VaccinationTypeSummary(vt.id(), vt.name()),
            dto.lot(), dto.notes(), dto.route(), dto.applicationSite(), dto.nextVaccination(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            co == null ? null : new ConsultationSummary(co.id(), co.date()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate(),
            dto.enabled());
    }
}
