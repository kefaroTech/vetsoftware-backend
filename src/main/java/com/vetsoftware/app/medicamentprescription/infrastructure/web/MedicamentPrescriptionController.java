package com.vetsoftware.app.medicamentprescription.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.dto.PrescriptionSummaryDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.request.CreateMedicamentPrescriptionRequest;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.response.MedicamentPrescriptionResponse;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.response.PrescriptionSummary;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicament-prescriptions")
public class MedicamentPrescriptionController {
    private final CreateMedicamentPrescriptionUseCase createUseCase;
    private final ListMedicamentPrescriptionsUseCase listUseCase;
    private final Authz authz;

    public MedicamentPrescriptionController(CreateMedicamentPrescriptionUseCase createUseCase,
            ListMedicamentPrescriptionsUseCase listUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentPrescriptionResponse create(
            @Valid @RequestBody CreateMedicamentPrescriptionRequest request) {
        return toResponse(createUseCase.execute(new CreateMedicamentPrescriptionCommand(
                request.medicamentId(), request.presentation(), request.quantity(),
                request.posology(), request.observation(), request.prescriptionId(),
                authz.currentCompanyIdOrNull())));
    }

    @GetMapping
    public PageResponse<MedicamentPrescriptionResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(page, pageSize), this::toResponse);
    }

    private MedicamentPrescriptionResponse toResponse(MedicamentPrescriptionDto dto) {
        PrescriptionSummaryDto p = dto.prescription();
        return new MedicamentPrescriptionResponse(dto.id(), dto.medicamentId(), dto.name(),
                dto.presentation(), dto.quantity(), dto.posology(), dto.observation(),
                new PrescriptionSummary(p.id(), p.date()), dto.createdDate(), dto.enabled());
    }
}
