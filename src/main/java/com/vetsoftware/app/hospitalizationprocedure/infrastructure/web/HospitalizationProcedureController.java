package com.vetsoftware.app.hospitalizationprocedure.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationprocedure.application.command.CreateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.SuspendHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.command.UpdateHospitalizationProcedureCommand;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationProcedureDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.CreateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.DeleteHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.FindHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ListHospitalizationProceduresByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.ReactivateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.SuspendHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.application.port.in.UpdateHospitalizationProcedureUseCase;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.request.CreateHospitalizationProcedureRequest;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.request.UpdateHospitalizationProcedureRequest;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.response.HospitalizationProcedureResponse;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.web.response.HospitalizationSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospitalization-procedures")
public class HospitalizationProcedureController {
    private final CreateHospitalizationProcedureUseCase createUseCase;
    private final UpdateHospitalizationProcedureUseCase updateUseCase;
    private final FindHospitalizationProcedureUseCase findUseCase;
    private final ListHospitalizationProceduresByHospitalizationUseCase listByHospitalizationUseCase;
    private final DeleteHospitalizationProcedureUseCase deleteUseCase;
    private final ReactivateHospitalizationProcedureUseCase reactivateUseCase;
    private final SuspendHospitalizationProcedureUseCase suspendUseCase;
    private final Authz authz;

    public HospitalizationProcedureController(CreateHospitalizationProcedureUseCase createUseCase,
            UpdateHospitalizationProcedureUseCase updateUseCase,
            FindHospitalizationProcedureUseCase findUseCase,
            ListHospitalizationProceduresByHospitalizationUseCase listByHospitalizationUseCase,
            DeleteHospitalizationProcedureUseCase deleteUseCase,
            ReactivateHospitalizationProcedureUseCase reactivateUseCase,
            SuspendHospitalizationProcedureUseCase suspendUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.suspendUseCase = suspendUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalizationProcedureResponse create(
            @Valid @RequestBody CreateHospitalizationProcedureRequest request) {
        return toResponse(createUseCase
                .execute(new CreateHospitalizationProcedureCommand(request.name(), request.dose(),
                        request.frequency(), request.guidelineType(), request.durationMeasure(),
                        request.durationQuantity(), request.startDate(), request.startTime(),
                        request.notes(), request.hospitalizationId(), authz.currentEmployeeId())));
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public PageResponse<HospitalizationProcedureResponse> listByHospitalization(
            @PathVariable Long hospitalizationId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listByHospitalizationUseCase.listByHospitalization(
                hospitalizationId, authz.currentCompanyId(), page, pageSize), this::toResponse);
    }

    @GetMapping("/{id}")
    public HospitalizationProcedureResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public HospitalizationProcedureResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateHospitalizationProcedureRequest request) {
        return toResponse(updateUseCase.execute(new UpdateHospitalizationProcedureCommand(id,
                request.name(), request.dose(), request.frequency(), request.guidelineType(),
                request.durationMeasure(), request.durationQuantity(), request.startDate(),
                request.startTime(), request.notes())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public HospitalizationProcedureResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    /**
     * Suspende la orden (registra quién/cuándo). El plan de ejecuciones pendientes
     * se limpia aparte.
     */
    @PatchMapping("/{id}/suspend")
    public HospitalizationProcedureResponse suspend(@PathVariable Long id) {
        return toResponse(suspendUseCase.execute(
                new SuspendHospitalizationProcedureCommand(id, authz.currentEmployeeId())));
    }

    private HospitalizationProcedureResponse toResponse(HospitalizationProcedureDto dto) {
        HospitalizationSummaryDto h = dto.hospitalization();
        EmployeeSummaryDto c = dto.createdBy();
        EmployeeSummaryDto s = dto.suspensionBy();
        return new HospitalizationProcedureResponse(dto.id(), dto.name(), dto.dose(),
                dto.frequency(), dto.guidelineType(), dto.durationMeasure(), dto.durationQuantity(),
                dto.startDate(), dto.startTime(), dto.notes(),
                new HospitalizationSummary(h.id(), h.date()),
                new EmployeeSummary(c.id(), c.employeeCode(), c.name()), dto.createdDate(),
                dto.enabled(), dto.suspensionDate(),
                s == null ? null : new EmployeeSummary(s.id(), s.employeeCode(), s.name()));
    }
}
