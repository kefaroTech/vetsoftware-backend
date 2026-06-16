package com.vetsoftware.app.owner.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.CitySummaryDto;
import com.vetsoftware.app.owner.application.dto.CompanySummaryDto;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.CreateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.DeleteOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.FindOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.ListOwnersUseCase;
import com.vetsoftware.app.owner.application.port.in.ReactivateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.SearchOwnersUseCase;
import com.vetsoftware.app.owner.application.port.in.UpdateOwnerUseCase;
import com.vetsoftware.app.owner.infrastructure.web.request.CreateOwnerRequest;
import com.vetsoftware.app.owner.infrastructure.web.request.UpdateOwnerRequest;
import com.vetsoftware.app.owner.infrastructure.web.response.CitySummary;
import com.vetsoftware.app.owner.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.owner.infrastructure.web.response.OwnerResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/owners")
public class OwnerController {
    private final CreateOwnerUseCase createUseCase;
    private final UpdateOwnerUseCase updateUseCase;
    private final FindOwnerUseCase findUseCase;
    private final ListOwnersUseCase listUseCase;
    private final SearchOwnersUseCase searchUseCase;
    private final DeleteOwnerUseCase deleteUseCase;
    private final ReactivateOwnerUseCase reactivateUseCase;
    private final Authz authz;

    public OwnerController(CreateOwnerUseCase createUseCase, UpdateOwnerUseCase updateUseCase,
                           FindOwnerUseCase findUseCase, ListOwnersUseCase listUseCase,
                           SearchOwnersUseCase searchUseCase, DeleteOwnerUseCase deleteUseCase,
                           ReactivateOwnerUseCase reactivateUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(@Valid @RequestBody CreateOwnerRequest request) {
        return toResponse(createUseCase.execute(
            new CreateOwnerCommand(request.name(), request.email(), request.document(),
                request.documentType(), request.personType(), request.verificationDigit(),
                request.legalName(), request.address(), request.phone(), request.cityId(),
                authz.currentCompanyId(), request.withholdingAgent())));
    }

    @GetMapping
    public List<OwnerResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public List<OwnerResponse> search(@RequestParam("q") String query) {
        return searchUseCase.search(authz.currentCompanyId(), query)
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public OwnerResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOwnerRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateOwnerCommand(id, request.name(), request.email(), request.document(),
                request.documentType(), request.personType(), request.verificationDigit(),
                request.legalName(), request.address(), request.phone(), request.cityId(),
                request.companyId(), request.withholdingAgent())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public OwnerResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private OwnerResponse toResponse(OwnerDto dto) {
        CitySummaryDto c = dto.city();
        CompanySummaryDto co = dto.company();
        return new OwnerResponse(
            dto.id(), dto.name(), dto.email(), dto.document(), dto.documentType(),
            dto.personType(), dto.verificationDigit(), dto.legalName(), dto.address(), dto.phone(),
            new CitySummary(c.id(), c.name()),
            new CompanySummary(co.id(), co.name(), co.identifier()),
            dto.withholdingAgent(), dto.createdDate(), dto.enabled()
        );
    }
}
