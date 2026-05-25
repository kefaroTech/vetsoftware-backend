package com.vetsoftware.app.medicamentprescription.infrastructure.web;

import com.vetsoftware.app.medicamentprescription.application.command.CreateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.command.UpdateMedicamentPrescriptionCommand;
import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.dto.PrescriptionSummaryDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.CreateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.DeleteMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.FindMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ListMedicamentPrescriptionsUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.ReactivateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.in.UpdateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.request.CreateMedicamentPrescriptionRequest;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.request.UpdateMedicamentPrescriptionRequest;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.response.MedicamentPrescriptionResponse;
import com.vetsoftware.app.medicamentprescription.infrastructure.web.response.PrescriptionSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicament-prescriptions")
public class MedicamentPrescriptionController {
    private final CreateMedicamentPrescriptionUseCase createUseCase;
    private final UpdateMedicamentPrescriptionUseCase updateUseCase;
    private final FindMedicamentPrescriptionUseCase findUseCase;
    private final ListMedicamentPrescriptionsUseCase listUseCase;
    private final DeleteMedicamentPrescriptionUseCase deleteUseCase;
    private final ReactivateMedicamentPrescriptionUseCase reactivateUseCase;

    public MedicamentPrescriptionController(CreateMedicamentPrescriptionUseCase createUseCase,
                                            UpdateMedicamentPrescriptionUseCase updateUseCase,
                                            FindMedicamentPrescriptionUseCase findUseCase,
                                            ListMedicamentPrescriptionsUseCase listUseCase,
                                            DeleteMedicamentPrescriptionUseCase deleteUseCase,
                                            ReactivateMedicamentPrescriptionUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentPrescriptionResponse create(@Valid @RequestBody CreateMedicamentPrescriptionRequest request) {
        return toResponse(createUseCase.execute(
            new CreateMedicamentPrescriptionCommand(
                request.name(), request.presentation(), request.quantity(),
                request.posology(), request.prescriptionId())));
    }

    @GetMapping
    public List<MedicamentPrescriptionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MedicamentPrescriptionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public MedicamentPrescriptionResponse update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateMedicamentPrescriptionRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateMedicamentPrescriptionCommand(
                id, request.name(), request.presentation(), request.quantity(),
                request.posology(), request.prescriptionId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public MedicamentPrescriptionResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private MedicamentPrescriptionResponse toResponse(MedicamentPrescriptionDto dto) {
        PrescriptionSummaryDto p = dto.prescription();
        return new MedicamentPrescriptionResponse(
            dto.id(), dto.name(), dto.presentation(), dto.quantity(), dto.posology(),
            new PrescriptionSummary(p.id(), p.date()),
            dto.createdDate(),
            dto.enabled());
    }
}
