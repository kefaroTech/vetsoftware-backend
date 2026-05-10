package com.vetsoftware.app.spa.infrastructure.web;

import com.vetsoftware.app.spa.application.command.CreateSpaCommand;
import com.vetsoftware.app.spa.application.command.UpdateSpaCommand;
import com.vetsoftware.app.spa.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.spa.application.dto.CompanySummaryDto;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.dto.SpaTypeSummaryDto;
import com.vetsoftware.app.spa.application.port.in.CreateSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.DeleteSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.FindSpaUseCase;
import com.vetsoftware.app.spa.application.port.in.ListSpasUseCase;
import com.vetsoftware.app.spa.application.port.in.UpdateSpaUseCase;
import com.vetsoftware.app.spa.infrastructure.web.request.CreateSpaRequest;
import com.vetsoftware.app.spa.infrastructure.web.request.UpdateSpaRequest;
import com.vetsoftware.app.spa.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.spa.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.spa.infrastructure.web.response.SpaResponse;
import com.vetsoftware.app.spa.infrastructure.web.response.SpaTypeSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spas")
public class SpaController {
    private final CreateSpaUseCase createUseCase;
    private final UpdateSpaUseCase updateUseCase;
    private final FindSpaUseCase findUseCase;
    private final ListSpasUseCase listUseCase;
    private final DeleteSpaUseCase deleteUseCase;

    public SpaController(CreateSpaUseCase createUseCase,
                         UpdateSpaUseCase updateUseCase,
                         FindSpaUseCase findUseCase,
                         ListSpasUseCase listUseCase,
                         DeleteSpaUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaResponse create(@Valid @RequestBody CreateSpaRequest request) {
        return toResponse(createUseCase.execute(
            new CreateSpaCommand(
                request.date(), request.spaTypeId(), request.reason(),
                request.details(), request.observations(),
                request.animalId(), request.companyId())));
    }

    @GetMapping
    public List<SpaResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SpaResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SpaResponse update(@PathVariable Long id,
                              @Valid @RequestBody UpdateSpaRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateSpaCommand(
                id, request.date(), request.spaTypeId(), request.reason(),
                request.details(), request.observations(),
                request.animalId(), request.companyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private SpaResponse toResponse(SpaDto dto) {
        SpaTypeSummaryDto st = dto.spaType();
        AnimalSummaryDto a = dto.animal();
        CompanySummaryDto c = dto.company();
        return new SpaResponse(
            dto.id(), dto.date(),
            new SpaTypeSummary(st.id(), st.name()),
            dto.reason(), dto.details(), dto.observations(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.createdDate());
    }
}
