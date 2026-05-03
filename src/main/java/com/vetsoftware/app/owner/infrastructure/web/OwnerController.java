package com.vetsoftware.app.owner.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.owner.application.command.CreateOwnerCommand;
import com.vetsoftware.app.owner.application.command.UpdateOwnerCommand;
import com.vetsoftware.app.owner.application.dto.CitySummaryDto;
import com.vetsoftware.app.owner.application.dto.CompanySummaryDto;
import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.in.CreateOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.DeleteOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.FindOwnerUseCase;
import com.vetsoftware.app.owner.application.port.in.ListOwnersUseCase;
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
    private final DeleteOwnerUseCase deleteUseCase;

    public OwnerController(CreateOwnerUseCase createUseCase, UpdateOwnerUseCase updateUseCase,
                           FindOwnerUseCase findUseCase, ListOwnersUseCase listUseCase,
                           DeleteOwnerUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(@Valid @RequestBody CreateOwnerRequest request,
                                @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateOwnerCommand(request.name(), request.email(), request.document(),
                request.address(), request.phone(), request.cityId(), request.companyId()),
            authContext));
    }

    @GetMapping
    public List<OwnerResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public OwnerResponse findById(@PathVariable Long id,
                                  @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public OwnerResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOwnerRequest request,
                                @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateOwnerCommand(id, request.name(), request.email(), request.document(),
                request.address(), request.phone(), request.cityId(), request.companyId()),
            authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private OwnerResponse toResponse(OwnerDto dto) {
        CitySummaryDto c = dto.city();
        CompanySummaryDto co = dto.company();
        return new OwnerResponse(
            dto.id(), dto.name(), dto.email(), dto.document(), dto.address(), dto.phone(),
            new CitySummary(c.id(), c.name()),
            new CompanySummary(co.id(), co.name(), co.identifier()),
            dto.createdDate()
        );
    }
}
