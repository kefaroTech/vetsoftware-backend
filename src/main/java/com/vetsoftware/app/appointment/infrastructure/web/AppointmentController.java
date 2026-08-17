package com.vetsoftware.app.appointment.infrastructure.web;

import com.vetsoftware.app.appointment.application.command.CancelAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.ChangeAppointmentStatusCommand;
import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.command.UpdateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.dto.BranchSummaryDto;
import com.vetsoftware.app.appointment.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.appointment.application.dto.OwnerSummaryDto;
import com.vetsoftware.app.appointment.application.port.in.CancelAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.ChangeAppointmentStatusUseCase;
import com.vetsoftware.app.appointment.application.port.in.CreateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.DeleteAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.GetAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.ListAppointmentsUseCase;
import com.vetsoftware.app.appointment.application.port.in.RescheduleAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.in.UpdateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.infrastructure.web.request.CancelAppointmentRequest;
import com.vetsoftware.app.appointment.infrastructure.web.request.ChangeAppointmentStatusRequest;
import com.vetsoftware.app.appointment.infrastructure.web.request.CreateAppointmentRequest;
import com.vetsoftware.app.appointment.infrastructure.web.request.RescheduleAppointmentRequest;
import com.vetsoftware.app.appointment.infrastructure.web.request.UpdateAppointmentRequest;
import com.vetsoftware.app.appointment.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.appointment.infrastructure.web.response.AppointmentResponse;
import com.vetsoftware.app.appointment.infrastructure.web.response.BranchSummary;
import com.vetsoftware.app.appointment.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.appointment.infrastructure.web.response.OwnerSummary;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final CreateAppointmentUseCase createUseCase;
    private final ListAppointmentsUseCase listUseCase;
    private final GetAppointmentUseCase getUseCase;
    private final UpdateAppointmentUseCase updateUseCase;
    private final RescheduleAppointmentUseCase rescheduleUseCase;
    private final ChangeAppointmentStatusUseCase changeStatusUseCase;
    private final CancelAppointmentUseCase cancelUseCase;
    private final DeleteAppointmentUseCase deleteUseCase;
    private final Authz authz;

    public AppointmentController(CreateAppointmentUseCase createUseCase,
            ListAppointmentsUseCase listUseCase, GetAppointmentUseCase getUseCase,
            UpdateAppointmentUseCase updateUseCase, RescheduleAppointmentUseCase rescheduleUseCase,
            ChangeAppointmentStatusUseCase changeStatusUseCase,
            CancelAppointmentUseCase cancelUseCase, DeleteAppointmentUseCase deleteUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.updateUseCase = updateUseCase;
        this.rescheduleUseCase = rescheduleUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.cancelUseCase = cancelUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) {
        return toResponse(createUseCase.execute(new CreateAppointmentCommand(request.startAt(),
                request.durationMinutes(), request.type(), request.employeeId(), request.animalId(),
                request.ownerId(), request.clientName(), request.clientPhone(),
                request.clientEmail(), request.notes(),
                authz.resolveAccessibleBranch(request.branchId()), authz.currentCompanyId(),
                request.forceOverlap(), authz.currentBranchIdsOrEmpty())));
    }

    @GetMapping
    public List<AppointmentResponse> list(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "employeeId", required = false) Long employeeId,
            @RequestParam(name = "status", required = false) AppointmentStatus status,
            @RequestParam(name = "branchId", required = false) Long branchId) {
        return listUseCase
                .execute(new ListAppointmentsQuery(authz.currentCompanyId(), from, to, employeeId,
                        status, authz.resolveAccessibleBranch(branchId)))
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public AppointmentResponse findById(@PathVariable Long id) {
        return toResponse(getUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return toResponse(updateUseCase.execute(new UpdateAppointmentCommand(id, request.startAt(),
                request.durationMinutes(), request.type(), request.employeeId(), request.animalId(),
                request.ownerId(), request.clientName(), request.clientPhone(),
                request.clientEmail(), request.notes(), authz.currentCompanyId(),
                request.forceOverlap(), authz.currentBranchIdsOrEmpty())));
    }

    @PatchMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return toResponse(
                rescheduleUseCase.execute(new RescheduleAppointmentCommand(id, request.startAt(),
                        request.durationMinutes(), request.employeeId(), authz.currentCompanyId(),
                        request.forceOverlap(), authz.currentBranchIdsOrEmpty())));
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeAppointmentStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(new ChangeAppointmentStatusCommand(id,
                request.status(), authz.currentCompanyId())));
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id,
            @RequestBody(required = false) CancelAppointmentRequest request) {
        String reason = request == null ? null : request.reason();
        return toResponse(cancelUseCase
                .execute(new CancelAppointmentCommand(id, reason, authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    private AppointmentResponse toResponse(AppointmentDto dto) {
        AnimalSummaryDto a = dto.animal();
        OwnerSummaryDto o = dto.owner();
        EmployeeSummaryDto e = dto.employee();
        BranchSummaryDto b = dto.branch();
        return new AppointmentResponse(dto.id(), dto.startAt(), dto.durationMinutes(), dto.type(),
                dto.status(), dto.notes(), dto.cancellationReason(),
                a == null ? null : new AnimalSummary(a.id(), a.name(), a.code()),
                o == null ? null : new OwnerSummary(o.id(), o.name()), dto.clientName(),
                dto.clientPhone(), dto.clientEmail(), new EmployeeSummary(e.id(), e.name()),
                new BranchSummary(b.id(), b.name(), b.code()), dto.version(), dto.enabled(),
                dto.createdDate(), dto.overlappingAppointmentIds());
    }
}
