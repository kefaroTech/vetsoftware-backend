package com.vetsoftware.app.laboratorytest.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.laboratorytest.application.command.ChangeLaboratoryTestStatusCommand;
import com.vetsoftware.app.laboratorytest.application.command.CreateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.command.SearchLaboratoryTestsCommand;
import com.vetsoftware.app.laboratorytest.application.command.UpdateLaboratoryTestCommand;
import com.vetsoftware.app.laboratorytest.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.CompanySummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestTypeSummaryDto;
import com.vetsoftware.app.laboratorytest.application.dto.PageResult;
import com.vetsoftware.app.laboratorytest.application.port.in.ChangeLaboratoryTestStatusUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.CreateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.DeleteLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.FindLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsByAnimalUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ListLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.ReactivateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.SearchLaboratoryTestsUseCase;
import com.vetsoftware.app.laboratorytest.application.port.in.UpdateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.ChangeLaboratoryTestStatusRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.CreateLaboratoryTestRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.request.UpdateLaboratoryTestRequest;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.LaboratoryTestResponse;
import com.vetsoftware.app.laboratorytest.infrastructure.web.response.LaboratoryTestTypeSummary;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/laboratory-tests")
public class LaboratoryTestController {
    private final CreateLaboratoryTestUseCase createUseCase;
    private final UpdateLaboratoryTestUseCase updateUseCase;
    private final ChangeLaboratoryTestStatusUseCase changeStatusUseCase;
    private final FindLaboratoryTestUseCase findUseCase;
    private final ListLaboratoryTestsUseCase listUseCase;
    private final ListLaboratoryTestsByAnimalUseCase listByAnimalUseCase;
    private final SearchLaboratoryTestsUseCase searchUseCase;
    private final DeleteLaboratoryTestUseCase deleteUseCase;
    private final ReactivateLaboratoryTestUseCase reactivateUseCase;
    private final Authz authz;

    public LaboratoryTestController(CreateLaboratoryTestUseCase createUseCase,
            UpdateLaboratoryTestUseCase updateUseCase,
            ChangeLaboratoryTestStatusUseCase changeStatusUseCase,
            FindLaboratoryTestUseCase findUseCase, ListLaboratoryTestsUseCase listUseCase,
            ListLaboratoryTestsByAnimalUseCase listByAnimalUseCase,
            SearchLaboratoryTestsUseCase searchUseCase, DeleteLaboratoryTestUseCase deleteUseCase,
            ReactivateLaboratoryTestUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByAnimalUseCase = listByAnimalUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LaboratoryTestResponse create(@Valid @RequestBody CreateLaboratoryTestRequest request) {
        return toResponse(createUseCase.execute(new CreateLaboratoryTestCommand(request.date(),
                request.testTypeId(), request.quantity(), request.diagnosis(), request.status(),
                request.prioridad(), request.animalId(), request.consultationId(),
                authz.currentCompanyId(), authz.resolveAccessibleBranch(request.branchId()),
                request.processedById(), request.processedDate())));
    }

    @GetMapping
    public List<LaboratoryTestResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-animal/{animalId}")
    public PageResponse<LaboratoryTestResponse> listByAnimal(@PathVariable Long animalId,
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<LaboratoryTestDto> result = listByAnimalUseCase.listByAnimal(animalId, query,
                page, pageSize);
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/search")
    public PageResponse<LaboratoryTestResponse> search(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Long animalId,
            @RequestParam(required = false) Long testTypeId,
            @RequestParam(required = false) String prioridad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<LaboratoryTestStatus> statusList = statuses == null
                ? List.of()
                : statuses.stream().map(s -> LaboratoryTestStatus.valueOf(s.toUpperCase()))
                        .toList();
        LaboratoryTestPriority priority = prioridad == null || prioridad.isBlank()
                ? null
                : LaboratoryTestPriority.valueOf(prioridad.toUpperCase());
        PageResult<LaboratoryTestDto> result = searchUseCase
                .execute(new SearchLaboratoryTestsCommand(authz.currentCompanyId(),
                        authz.resolveAccessibleBranch(branchId), statusList, animalId, testTypeId,
                        priority, dateFrom, dateTo, page, pageSize));
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public LaboratoryTestResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public LaboratoryTestResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateLaboratoryTestRequest request) {
        return toResponse(updateUseCase.execute(new UpdateLaboratoryTestCommand(id, request.date(),
                request.testTypeId(), request.quantity(), request.diagnosis(), request.prioridad(),
                request.animalId(), request.consultationId(), authz.currentCompanyId(),
                request.processedById(), request.processedDate())));
    }

    @PatchMapping("/{id}/status")
    public LaboratoryTestResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeLaboratoryTestStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(new ChangeLaboratoryTestStatusCommand(id,
                request.status(), authz.currentEmployeeIdOrNull())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public LaboratoryTestResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private LaboratoryTestResponse toResponse(LaboratoryTestDto dto) {
        LaboratoryTestTypeSummaryDto tt = dto.testType();
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        EmployeeSummaryDto pb = dto.processedBy();
        return new LaboratoryTestResponse(dto.id(), dto.date(),
                new LaboratoryTestTypeSummary(tt.id(), tt.name()), dto.quantity(), dto.diagnosis(),
                dto.status(), dto.prioridad(), new AnimalSummary(a.id(), a.name(), a.code()),
                co == null ? null : new ConsultationSummary(co.id(), co.date()),
                new CompanySummary(c.id(), c.name(), c.identifier()),
                pb == null ? null : new EmployeeSummary(pb.id(), pb.employeeCode(), pb.name()),
                dto.processedDate(), dto.createdDate(), dto.enabled());
    }
}
