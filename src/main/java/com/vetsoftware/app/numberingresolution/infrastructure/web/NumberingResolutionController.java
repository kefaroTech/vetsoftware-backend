package com.vetsoftware.app.numberingresolution.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.numberingresolution.application.command.CreateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.command.UpdateNumberingResolutionCommand;
import com.vetsoftware.app.numberingresolution.application.dto.CompanySummaryDto;
import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.CreateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.DeleteNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.FindNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.ListNumberingResolutionsUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.ReactivateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.in.UpdateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.infrastructure.web.request.CreateNumberingResolutionRequest;
import com.vetsoftware.app.numberingresolution.infrastructure.web.request.UpdateNumberingResolutionRequest;
import com.vetsoftware.app.numberingresolution.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.numberingresolution.infrastructure.web.response.NumberingResolutionResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/numbering-resolutions")
public class NumberingResolutionController {
    private final CreateNumberingResolutionUseCase createUseCase;
    private final UpdateNumberingResolutionUseCase updateUseCase;
    private final FindNumberingResolutionUseCase findUseCase;
    private final ListNumberingResolutionsUseCase listUseCase;
    private final DeleteNumberingResolutionUseCase deleteUseCase;
    private final ReactivateNumberingResolutionUseCase reactivateUseCase;
    private final Authz authz;

    public NumberingResolutionController(CreateNumberingResolutionUseCase createUseCase,
                                         UpdateNumberingResolutionUseCase updateUseCase,
                                         FindNumberingResolutionUseCase findUseCase,
                                         ListNumberingResolutionsUseCase listUseCase,
                                         DeleteNumberingResolutionUseCase deleteUseCase,
                                         ReactivateNumberingResolutionUseCase reactivateUseCase,
                                         Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NumberingResolutionResponse create(@Valid @RequestBody CreateNumberingResolutionRequest request) {
        return toResponse(createUseCase.execute(new CreateNumberingResolutionCommand(
                request.documentType(), request.resolutionNumber(), request.resolutionDate(), request.prefix(),
                request.rangeFrom(), request.rangeTo(), request.validFrom(), request.validTo(),
                request.technicalKey(), request.branchId(), authz.currentCompanyId())));
    }

    @GetMapping
    public List<NumberingResolutionResponse> listAll() {
        return listUseCase.listByCompany(authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public NumberingResolutionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public NumberingResolutionResponse update(@PathVariable Long id,
                                              @Valid @RequestBody UpdateNumberingResolutionRequest request) {
        return toResponse(updateUseCase.execute(new UpdateNumberingResolutionCommand(
                id, request.documentType(), request.resolutionNumber(), request.resolutionDate(), request.prefix(),
                request.rangeFrom(), request.rangeTo(), request.validFrom(), request.validTo(),
                request.technicalKey(), request.branchId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public NumberingResolutionResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private NumberingResolutionResponse toResponse(NumberingResolutionDto dto) {
        CompanySummaryDto c = dto.company();
        return new NumberingResolutionResponse(
                dto.id(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.branchId(),
                dto.documentType(),
                dto.resolutionNumber(),
                dto.resolutionDate(),
                dto.prefix(),
                dto.rangeFrom(),
                dto.rangeTo(),
                dto.validFrom(),
                dto.validTo(),
                dto.technicalKey(),
                dto.currentNumber(),
                dto.createdDate(),
                dto.enabled());
    }
}
