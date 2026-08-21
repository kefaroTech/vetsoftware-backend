package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.hospitalizationprogressnote.application.command.CreateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationSummaryDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.CreateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.DeleteHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ListHospitalizationProgressNotesByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.request.CreateHospitalizationProgressNoteRequest;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response.HospitalizationProgressNoteResponse;
import com.vetsoftware.app.hospitalizationprogressnote.infrastructure.web.response.HospitalizationSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hospitalization-progress-notes")
public class HospitalizationProgressNoteController {
    private final CreateHospitalizationProgressNoteUseCase createUseCase;
    private final ListHospitalizationProgressNotesByHospitalizationUseCase listByHospitalizationUseCase;
    private final DeleteHospitalizationProgressNoteUseCase deleteUseCase;
    private final Authz authz;

    public HospitalizationProgressNoteController(
            CreateHospitalizationProgressNoteUseCase createUseCase,
            ListHospitalizationProgressNotesByHospitalizationUseCase listByHospitalizationUseCase,
            DeleteHospitalizationProgressNoteUseCase deleteUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listByHospitalizationUseCase = listByHospitalizationUseCase;
        this.deleteUseCase = deleteUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HospitalizationProgressNoteResponse create(
            @Valid @RequestBody CreateHospitalizationProgressNoteRequest request) {
        return toResponse(createUseCase.execute(new CreateHospitalizationProgressNoteCommand(
                request.description(), request.hospitalizationId(), authz.currentEmployeeId(),
                authz.currentCompanyId())));
    }

    @GetMapping("/by-hospitalization/{hospitalizationId}")
    public PageResponse<HospitalizationProgressNoteResponse> listByHospitalization(
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

    private HospitalizationProgressNoteResponse toResponse(HospitalizationProgressNoteDto dto) {
        HospitalizationSummaryDto h = dto.hospitalization();
        EmployeeSummaryDto c = dto.createdBy();
        return new HospitalizationProgressNoteResponse(dto.id(), dto.description(),
                new HospitalizationSummary(h.id(), h.date()),
                new EmployeeSummary(c.id(), c.employeeCode(), c.name()), dto.createdDate(),
                dto.enabled());
    }
}
