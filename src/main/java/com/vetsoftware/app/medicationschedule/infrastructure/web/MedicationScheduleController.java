package com.vetsoftware.app.medicationschedule.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.medicationschedule.application.command.ApplyMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.command.GenerateMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.command.RescheduleMedicationScheduleCommand;
import com.vetsoftware.app.medicationschedule.application.dto.MedicationScheduleDto;
import com.vetsoftware.app.medicationschedule.application.port.in.ApplyMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.GenerateMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.ListMedicationSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.RescheduleMedicationScheduleUseCase;
import com.vetsoftware.app.medicationschedule.application.port.in.SuspendPendingMedicationSchedulesUseCase;
import com.vetsoftware.app.medicationschedule.infrastructure.web.request.RescheduleMedicationScheduleRequest;
import com.vetsoftware.app.medicationschedule.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.medicationschedule.infrastructure.web.response.HospitalizationMedicationSummary;
import com.vetsoftware.app.medicationschedule.infrastructure.web.response.MedicationScheduleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medication-schedules")
public class MedicationScheduleController {
    private final GenerateMedicationScheduleUseCase generateUseCase;
    private final ListMedicationSchedulesByHospitalizationUseCase listByHospitalizationUseCase;
    private final ApplyMedicationScheduleUseCase applyUseCase;
    private final RescheduleMedicationScheduleUseCase rescheduleUseCase;
    private final SuspendPendingMedicationSchedulesUseCase suspendPendingUseCase;
    private final Authz authz;

    public MedicationScheduleController(GenerateMedicationScheduleUseCase generateUseCase,
                                        ListMedicationSchedulesByHospitalizationUseCase listByHospitalizationUseCase,
                                        ApplyMedicationScheduleUseCase applyUseCase,
                                        RescheduleMedicationScheduleUseCase rescheduleUseCase,
                                        SuspendPendingMedicationSchedulesUseCase suspendPendingUseCase,
                                        Authz authz) {
        this.generateUseCase = generateUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.applyUseCase = applyUseCase;
        this.rescheduleUseCase = rescheduleUseCase;
        this.suspendPendingUseCase = suspendPendingUseCase;
        this.authz = authz;
    }

    @PostMapping("/generate/{hospitalizationMedicationId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<MedicationScheduleResponse> generate(@PathVariable Long hospitalizationMedicationId) {
        return generateUseCase.execute(
                new GenerateMedicationScheduleCommand(hospitalizationMedicationId, authz.currentEmployeeId()))
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public List<MedicationScheduleResponse> listByHospitalization(@PathVariable Long hospitalizationId) {
        return listByHospitalizationUseCase.listByHospitalization(hospitalizationId)
            .stream().map(this::toResponse).toList();
    }

    /** Marca una toma como aplicada (hora real = ahora). Devuelve el plan de esa medicación. */
    @PatchMapping("/{id}/apply")
    public List<MedicationScheduleResponse> apply(@PathVariable Long id) {
        return applyUseCase.execute(new ApplyMedicationScheduleCommand(id))
            .stream().map(this::toResponse).toList();
    }

    /** Reprograma una toma (mode=one|cascade). Devuelve el plan de esa medicación. */
    @PatchMapping("/{id}/reschedule")
    public List<MedicationScheduleResponse> reschedule(@PathVariable Long id,
                                                       @Valid @RequestBody RescheduleMedicationScheduleRequest request) {
        return rescheduleUseCase.execute(
                new RescheduleMedicationScheduleCommand(id, request.newDateTime(), request.mode()))
            .stream().map(this::toResponse).toList();
    }

    /** Soft-delete de las tomas pendientes (al suspender la medicación). Devuelve las aplicadas. */
    @PatchMapping("/by-medication/{hospitalizationMedicationId}/suspend-pending")
    public List<MedicationScheduleResponse> suspendPending(@PathVariable Long hospitalizationMedicationId) {
        return suspendPendingUseCase.execute(hospitalizationMedicationId)
            .stream().map(this::toResponse).toList();
    }

    private MedicationScheduleResponse toResponse(MedicationScheduleDto dto) {
        return new MedicationScheduleResponse(
            dto.id(),
            new HospitalizationMedicationSummary(dto.hospitalizationMedicationId(), dto.hospitalizationMedicationName()),
            dto.originalDateTime(),
            dto.currentDateTime(),
            dto.realDateTime(),
            dto.appliedStatus(),
            dto.rescheduled(),
            new EmployeeSummary(dto.createdById(), dto.createdByCode(), dto.createdByName()),
            dto.createdDate(),
            dto.enabled());
    }
}
