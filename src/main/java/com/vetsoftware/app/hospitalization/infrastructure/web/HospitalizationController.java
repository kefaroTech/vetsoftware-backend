package com.vetsoftware.app.hospitalization.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalization.application.command.CreateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.command.UpdateHospitalizationCommand;
import com.vetsoftware.app.hospitalization.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.CompanySummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.hospitalization.application.port.in.CreateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.DeleteHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.FindHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByAnimalUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsByCompanyUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsUseCase;
import com.vetsoftware.app.hospitalization.application.port.in.UpdateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.infrastructure.web.request.CreateHospitalizationRequest;
import com.vetsoftware.app.hospitalization.infrastructure.web.request.UpdateHospitalizationRequest;
import com.vetsoftware.app.hospitalization.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.hospitalization.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.hospitalization.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.hospitalization.infrastructure.web.response.HospitalizationResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospitalizations")
public class HospitalizationController {
    private final CreateHospitalizationUseCase createUseCase;
    private final UpdateHospitalizationUseCase updateUseCase;
    private final FindHospitalizationUseCase findUseCase;
    private final ListHospitalizationsUseCase listUseCase;
    private final ListHospitalizationsByCompanyUseCase listByCompanyUseCase;
    private final ListHospitalizationsByAnimalUseCase listByAnimalUseCase;
    private final DeleteHospitalizationUseCase deleteUseCase;
    private final Authz authz;

    public HospitalizationController(CreateHospitalizationUseCase createUseCase,
            UpdateHospitalizationUseCase updateUseCase, FindHospitalizationUseCase findUseCase,
            ListHospitalizationsUseCase listUseCase,
            ListHospitalizationsByAnimalUseCase listByAnimalUseCase,
            ListHospitalizationsByCompanyUseCase listByCompanyUseCase,
            DeleteHospitalizationUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalizationResponse create(
            @Valid @RequestBody CreateHospitalizationRequest request) {
        return toResponse(createUseCase.execute(new CreateHospitalizationCommand(request.date(),
                request.startDate(), request.endDate(), request.type(), request.reasonLeaving(),
                request.reason(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId(), request.weight(),
                request.weightUnit())));
    }

    @GetMapping
    public List<HospitalizationResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-company")
    public List<HospitalizationResponse> listByCompany() {
        return listByCompanyUseCase.listByCompany(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public PageResponse<HospitalizationResponse> listByAnimal(@PathVariable Long animalId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByAnimalUseCase.listByAnimal(animalId,
                authz.currentCompanyId(), query, page, pageSize), this::toResponse);
    }

    @GetMapping("/{id}")
    public HospitalizationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public HospitalizationResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateHospitalizationRequest request) {
        return toResponse(updateUseCase.execute(new UpdateHospitalizationCommand(id, request.date(),
                request.startDate(), request.endDate(), request.type(), request.reasonLeaving(),
                request.reason(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private HospitalizationResponse toResponse(HospitalizationDto dto) {
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        return new HospitalizationResponse(dto.id(), dto.date(), dto.startDate(), dto.endDate(),
                dto.type(), dto.reasonLeaving(), dto.reason(), dto.observations(),
                new AnimalSummary(a.id(), a.name(), a.code()),
                co == null ? null : new ConsultationSummary(co.id(), co.date()),
                new CompanySummary(c.id(), c.name(), c.identifier()), dto.createdDate(),
                dto.enabled());
    }
}
