package com.vetsoftware.app.hospitalizationmedication.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationmedication.application.command.CreateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.command.UpdateHospitalizationMedicationCommand;
import com.vetsoftware.app.hospitalizationmedication.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.CreateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.DeleteHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.FindHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ListHospitalizationMedicationsByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ReactivateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.UpdateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.web.request.CreateHospitalizationMedicationRequest;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.web.request.UpdateHospitalizationMedicationRequest;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.web.response.HospitalizationMedicationResponse;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.web.response.HospitalizationSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospitalization-medications")
public class HospitalizationMedicationController {
    private final CreateHospitalizationMedicationUseCase createUseCase;
    private final UpdateHospitalizationMedicationUseCase updateUseCase;
    private final FindHospitalizationMedicationUseCase findUseCase;
    private final ListHospitalizationMedicationsByHospitalizationUseCase listByHospitalizationUseCase;
    private final DeleteHospitalizationMedicationUseCase deleteUseCase;
    private final ReactivateHospitalizationMedicationUseCase reactivateUseCase;
    private final Authz authz;

    public HospitalizationMedicationController(CreateHospitalizationMedicationUseCase createUseCase,
                                               UpdateHospitalizationMedicationUseCase updateUseCase,
                                               FindHospitalizationMedicationUseCase findUseCase,
                                               ListHospitalizationMedicationsByHospitalizationUseCase listByHospitalizationUseCase,
                                               DeleteHospitalizationMedicationUseCase deleteUseCase,
                                               ReactivateHospitalizationMedicationUseCase reactivateUseCase,
                                               Authz authz) {
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
    public HospitalizationMedicationResponse create(@Valid @RequestBody CreateHospitalizationMedicationRequest request) {
        return toResponse(createUseCase.execute(new CreateHospitalizationMedicationCommand(
            request.name(), request.dose(), request.frequency(), request.guidelineType(),
            request.durationMeasure(), request.durationQuantity(), request.startDate(), request.startTime(),
            request.notes(), request.hospitalizationId(), authz.currentEmployeeId())));
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public List<HospitalizationMedicationResponse> listByHospitalization(@PathVariable Long hospitalizationId) {
        return listByHospitalizationUseCase.listByHospitalization(hospitalizationId).stream()
            .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public HospitalizationMedicationResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public HospitalizationMedicationResponse update(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateHospitalizationMedicationRequest request) {
        return toResponse(updateUseCase.execute(new UpdateHospitalizationMedicationCommand(
            id, request.name(), request.dose(), request.frequency(), request.guidelineType(),
            request.durationMeasure(), request.durationQuantity(), request.startDate(), request.startTime(),
            request.notes())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public HospitalizationMedicationResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private HospitalizationMedicationResponse toResponse(HospitalizationMedicationDto dto) {
        HospitalizationSummaryDto h = dto.hospitalization();
        EmployeeSummaryDto c = dto.createdBy();
        return new HospitalizationMedicationResponse(
            dto.id(), dto.name(), dto.dose(), dto.frequency(), dto.guidelineType(),
            dto.durationMeasure(), dto.durationQuantity(), dto.startDate(), dto.startTime(), dto.notes(),
            new HospitalizationSummary(h.id(), h.date()),
            new EmployeeSummary(c.id(), c.employeeCode(), c.name()),
            dto.createdDate(), dto.enabled());
    }
}
