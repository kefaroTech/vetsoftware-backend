package com.vetsoftware.app.procedureschedule.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.procedureschedule.application.command.ApplyProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.command.GenerateProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.command.RescheduleProcedureScheduleCommand;
import com.vetsoftware.app.procedureschedule.application.dto.ProcedureScheduleDto;
import com.vetsoftware.app.procedureschedule.application.port.in.ApplyProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.GenerateProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.ListProcedureSchedulesByHospitalizationUseCase;
import com.vetsoftware.app.procedureschedule.application.port.in.RescheduleProcedureScheduleUseCase;
import com.vetsoftware.app.procedureschedule.infrastructure.web.request.RescheduleProcedureScheduleRequest;
import com.vetsoftware.app.procedureschedule.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.procedureschedule.infrastructure.web.response.HospitalizationProcedureSummary;
import com.vetsoftware.app.procedureschedule.infrastructure.web.response.ProcedureScheduleResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/procedure-schedules")
public class ProcedureScheduleController {
    private final GenerateProcedureScheduleUseCase generateUseCase;
    private final ListProcedureSchedulesByHospitalizationUseCase listByHospitalizationUseCase;
    private final ApplyProcedureScheduleUseCase applyUseCase;
    private final RescheduleProcedureScheduleUseCase rescheduleUseCase;
    private final Authz authz;

    public ProcedureScheduleController(GenerateProcedureScheduleUseCase generateUseCase,
                                       ListProcedureSchedulesByHospitalizationUseCase listByHospitalizationUseCase,
                                       ApplyProcedureScheduleUseCase applyUseCase,
                                       RescheduleProcedureScheduleUseCase rescheduleUseCase,
                                       Authz authz) {
        this.generateUseCase = generateUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.applyUseCase = applyUseCase;
        this.rescheduleUseCase = rescheduleUseCase;
        this.authz = authz;
    }

    @PostMapping("/generate/{hospitalizationProcedureId}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ProcedureScheduleResponse> generate(@PathVariable Long hospitalizationProcedureId) {
        return generateUseCase.execute(
                new GenerateProcedureScheduleCommand(hospitalizationProcedureId, authz.currentEmployeeId()))
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public List<ProcedureScheduleResponse> listByHospitalization(@PathVariable Long hospitalizationId) {
        return listByHospitalizationUseCase.listByHospitalization(hospitalizationId)
            .stream().map(this::toResponse).toList();
    }

    /** Marca una ejecución como aplicada (hora real = ahora). Devuelve el plan de ese procedimiento. */
    @PatchMapping("/{id}/apply")
    public List<ProcedureScheduleResponse> apply(@PathVariable Long id) {
        return applyUseCase.execute(new ApplyProcedureScheduleCommand(id))
            .stream().map(this::toResponse).toList();
    }

    /** Reprograma una ejecución (mode=one|cascade). Devuelve el plan de ese procedimiento. */
    @PatchMapping("/{id}/reschedule")
    public List<ProcedureScheduleResponse> reschedule(@PathVariable Long id,
                                                      @Valid @RequestBody RescheduleProcedureScheduleRequest request) {
        return rescheduleUseCase.execute(
                new RescheduleProcedureScheduleCommand(id, request.newDateTime(), request.mode()))
            .stream().map(this::toResponse).toList();
    }

    private ProcedureScheduleResponse toResponse(ProcedureScheduleDto dto) {
        return new ProcedureScheduleResponse(
            dto.id(),
            new HospitalizationProcedureSummary(dto.hospitalizationProcedureId(), dto.hospitalizationProcedureName()),
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
