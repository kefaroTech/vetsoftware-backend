package com.vetsoftware.app.prescription.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.prescription.application.command.CreatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.command.UpdatePrescriptionCommand;
import com.vetsoftware.app.prescription.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.prescription.application.dto.CompanySummaryDto;
import com.vetsoftware.app.prescription.application.dto.ConsultationSummaryDto;
import com.vetsoftware.app.prescription.application.dto.PageResult;
import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.prescription.application.port.in.CreatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.DeletePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.ExportPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.FindPrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.ListPrescriptionsUseCase;
import com.vetsoftware.app.prescription.application.port.in.ReactivatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.in.UpdatePrescriptionUseCase;
import com.vetsoftware.app.prescription.infrastructure.web.request.CreatePrescriptionRequest;
import com.vetsoftware.app.prescription.infrastructure.web.request.UpdatePrescriptionRequest;
import com.vetsoftware.app.prescription.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.prescription.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.prescription.infrastructure.web.response.ConsultationSummary;
import com.vetsoftware.app.prescription.infrastructure.web.response.MedicamentSummary;
import com.vetsoftware.app.prescription.infrastructure.web.response.PrescriptionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prescriptions")
public class PrescriptionController {
    private final CreatePrescriptionUseCase createUseCase;
    private final UpdatePrescriptionUseCase updateUseCase;
    private final FindPrescriptionUseCase findUseCase;
    private final ListPrescriptionsUseCase listUseCase;
    private final DeletePrescriptionUseCase deleteUseCase;
    private final ReactivatePrescriptionUseCase reactivateUseCase;
    private final ExportPrescriptionUseCase exportUseCase;
    private final Authz authz;

    public PrescriptionController(CreatePrescriptionUseCase createUseCase,
            UpdatePrescriptionUseCase updateUseCase, FindPrescriptionUseCase findUseCase,
            ListPrescriptionsUseCase listUseCase, DeletePrescriptionUseCase deleteUseCase,
            ReactivatePrescriptionUseCase reactivateUseCase,
            ExportPrescriptionUseCase exportUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.exportUseCase = exportUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@Valid @RequestBody CreatePrescriptionRequest request) {
        return toResponse(createUseCase.execute(new CreatePrescriptionCommand(request.date(),
                request.diagnosis(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyId())));
    }

    @GetMapping
    public PageResponse<PrescriptionResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<PrescriptionDto> result = listUseCase.listAll(page, pageSize);
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public PrescriptionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public PrescriptionResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdatePrescriptionRequest request) {
        return toResponse(updateUseCase.execute(new UpdatePrescriptionCommand(id, request.date(),
                request.diagnosis(), request.observations(), request.animalId(),
                request.consultationId(), authz.currentCompanyIdOrNull())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyIdOrNull());
    }

    @PatchMapping("/{id}/enable")
    public PrescriptionResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    @GetMapping("/{id}/export.pdf")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] pdf = exportUseCase.execute(id, authz.currentCompanyId(),
                authz.currentEmployeeIdOrNull());
        String filename = "receta-" + id + ".pdf";
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private PrescriptionResponse toResponse(PrescriptionDto dto) {
        AnimalSummaryDto a = dto.animal();
        ConsultationSummaryDto co = dto.consultation();
        CompanySummaryDto c = dto.company();
        List<MedicamentSummary> medicaments = dto.medicaments().stream()
                .map(m -> new MedicamentSummary(m.id(), m.name(), m.presentation(), m.quantity(),
                        m.posology(), m.observation()))
                .toList();
        return new PrescriptionResponse(dto.id(), dto.date(), dto.diagnosis(), dto.observations(),
                new AnimalSummary(a.id(), a.name(), a.code()),
                new ConsultationSummary(co.id(), co.date()),
                new CompanySummary(c.id(), c.name(), c.identifier()), medicaments,
                dto.createdDate(), dto.enabled());
    }
}
