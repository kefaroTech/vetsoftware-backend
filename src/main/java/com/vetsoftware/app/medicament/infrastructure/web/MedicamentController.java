package com.vetsoftware.app.medicament.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.medicament.application.command.CreateMedicamentCommand;
import com.vetsoftware.app.medicament.application.command.UpdateMedicamentCommand;
import com.vetsoftware.app.medicament.application.dto.CompanySummaryDto;
import com.vetsoftware.app.medicament.application.dto.MedicamentDto;
import com.vetsoftware.app.medicament.application.port.in.CreateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.DeleteMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.FindMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListAvailableMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListDisabledMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ListMedicamentsUseCase;
import com.vetsoftware.app.medicament.application.port.in.ReactivateMedicamentUseCase;
import com.vetsoftware.app.medicament.application.port.in.UpdateMedicamentUseCase;
import com.vetsoftware.app.medicament.infrastructure.web.request.CreateMedicamentRequest;
import com.vetsoftware.app.medicament.infrastructure.web.request.UpdateMedicamentRequest;
import com.vetsoftware.app.medicament.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.medicament.infrastructure.web.response.MedicamentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/medicaments")
public class MedicamentController {
    private final CreateMedicamentUseCase createUseCase;
    private final UpdateMedicamentUseCase updateUseCase;
    private final FindMedicamentUseCase findUseCase;
    private final ListMedicamentsUseCase listUseCase;
    private final ListAvailableMedicamentsUseCase listAvailableUseCase;
    private final ListDisabledMedicamentsUseCase listDisabledUseCase;
    private final DeleteMedicamentUseCase deleteUseCase;
    private final ReactivateMedicamentUseCase reactivateUseCase;
    private final Authz authz;

    public MedicamentController(CreateMedicamentUseCase createUseCase,
            UpdateMedicamentUseCase updateUseCase, FindMedicamentUseCase findUseCase,
            ListMedicamentsUseCase listUseCase,
            ListAvailableMedicamentsUseCase listAvailableUseCase,
            ListDisabledMedicamentsUseCase listDisabledUseCase,
            DeleteMedicamentUseCase deleteUseCase, ReactivateMedicamentUseCase reactivateUseCase,
            Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listAvailableUseCase = listAvailableUseCase;
        this.listDisabledUseCase = listDisabledUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    // Los medicamentos creados por una empresa son propios de esa empresa (general
    // = false).
    // El catálogo global compartido (general = true) se siembra por migración.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicamentResponse create(@Valid @RequestBody CreateMedicamentRequest request) {
        return toResponse(createUseCase.execute(new CreateMedicamentCommand(request.name(),
                request.description(), authz.currentCompanyId(), false)));
    }

    @GetMapping
    public List<MedicamentResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/available")
    public List<MedicamentResponse> listAvailable() {
        return listAvailableUseCase.listAvailable(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/disabled")
    public List<MedicamentResponse> listDisabled() {
        return listDisabledUseCase.listDisabled(authz.currentCompanyId()).stream()
                .map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MedicamentResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public MedicamentResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateMedicamentRequest request) {
        return toResponse(updateUseCase
                .execute(new UpdateMedicamentCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public MedicamentResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private MedicamentResponse toResponse(MedicamentDto dto) {
        CompanySummaryDto c = dto.company();
        return new MedicamentResponse(dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(), dto.createdDate(), dto.enabled());
    }
}
