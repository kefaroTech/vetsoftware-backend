package com.vetsoftware.app.hospitalizationobservation.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.CreateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.DeleteHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ListHospitalizationObservationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request.CreateHospitalizationObservationRequest;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.response.HospitalizationObservationResponse;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.response.HospitalizationSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospitalization-observations")
public class HospitalizationObservationController {
    private final CreateHospitalizationObservationUseCase createUseCase;
    private final ListHospitalizationObservationsByHospitalizationUseCase listByHospitalizationUseCase;
    private final DeleteHospitalizationObservationUseCase deleteUseCase;
    private final Authz authz;

    public HospitalizationObservationController(
            CreateHospitalizationObservationUseCase createUseCase,
            ListHospitalizationObservationsByHospitalizationUseCase listByHospitalizationUseCase,
            DeleteHospitalizationObservationUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalizationObservationResponse create(
            @Valid @RequestBody CreateHospitalizationObservationRequest request) {
        return toResponse(createUseCase.execute(new CreateHospitalizationObservationCommand(
                request.description(), request.hospitalizationId(), authz.currentEmployeeId(),
                authz.currentCompanyId())));
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public PageResponse<HospitalizationObservationResponse> listByHospitalization(
            @PathVariable Long hospitalizationId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByHospitalizationUseCase.listByHospitalization(
                hospitalizationId, authz.currentCompanyId(), page, pageSize), this::toResponse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private HospitalizationObservationResponse toResponse(HospitalizationObservationDto dto) {
        HospitalizationSummaryDto h = dto.hospitalization();
        EmployeeSummaryDto c = dto.createdBy();
        return new HospitalizationObservationResponse(dto.id(), dto.description(),
                new HospitalizationSummary(h.id(), h.date()),
                new EmployeeSummary(c.id(), c.employeeCode(), c.name()), dto.createdDate(),
                dto.enabled());
    }
}
