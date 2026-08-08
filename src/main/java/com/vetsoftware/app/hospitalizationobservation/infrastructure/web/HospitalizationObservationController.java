package com.vetsoftware.app.hospitalizationobservation.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationobservation.application.command.CreateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.dto.PageResult;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.CreateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.DeleteHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.FindHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ListHospitalizationObservationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ReactivateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.UpdateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request.CreateHospitalizationObservationRequest;
import com.vetsoftware.app.hospitalizationobservation.infrastructure.web.request.UpdateHospitalizationObservationRequest;
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
    private final UpdateHospitalizationObservationUseCase updateUseCase;
    private final FindHospitalizationObservationUseCase findUseCase;
    private final ListHospitalizationObservationsByHospitalizationUseCase listByHospitalizationUseCase;
    private final DeleteHospitalizationObservationUseCase deleteUseCase;
    private final ReactivateHospitalizationObservationUseCase reactivateUseCase;
    private final Authz authz;

    public HospitalizationObservationController(
            CreateHospitalizationObservationUseCase createUseCase,
            UpdateHospitalizationObservationUseCase updateUseCase,
            FindHospitalizationObservationUseCase findUseCase,
            ListHospitalizationObservationsByHospitalizationUseCase listByHospitalizationUseCase,
            DeleteHospitalizationObservationUseCase deleteUseCase,
            ReactivateHospitalizationObservationUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalizationObservationResponse create(
            @Valid @RequestBody CreateHospitalizationObservationRequest request) {
        return toResponse(createUseCase.execute(new CreateHospitalizationObservationCommand(
                request.description(), request.hospitalizationId(), authz.currentEmployeeId())));
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public PageResponse<HospitalizationObservationResponse> listByHospitalization(
            @PathVariable Long hospitalizationId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<HospitalizationObservationDto> result = listByHospitalizationUseCase
                .listByHospitalization(hospitalizationId, page, pageSize);
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public HospitalizationObservationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public HospitalizationObservationResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateHospitalizationObservationRequest request) {
        return toResponse(updateUseCase
                .execute(new UpdateHospitalizationObservationCommand(id, request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public HospitalizationObservationResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
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
