package com.vetsoftware.app.permission.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.permission.application.command.CreatePermissionCommand;
import com.vetsoftware.app.permission.application.command.UpdatePermissionCommand;
import com.vetsoftware.app.permission.application.dto.CompanySummaryDto;
import com.vetsoftware.app.permission.application.dto.PermissionDto;
import com.vetsoftware.app.permission.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.permission.application.port.in.CreatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.DeletePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.FindPermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsByCompanyUseCase;
import com.vetsoftware.app.permission.application.port.in.ListPermissionsUseCase;
import com.vetsoftware.app.permission.application.port.in.ReactivatePermissionUseCase;
import com.vetsoftware.app.permission.application.port.in.UpdatePermissionUseCase;
import com.vetsoftware.app.permission.infrastructure.web.request.CreatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.request.UpdatePermissionRequest;
import com.vetsoftware.app.permission.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.permission.infrastructure.web.response.PermissionResponse;
import com.vetsoftware.app.permission.infrastructure.web.response.SubModuleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/permissions")
public class PermissionController {
    private final CreatePermissionUseCase createUseCase;
    private final UpdatePermissionUseCase updateUseCase;
    private final FindPermissionUseCase findUseCase;
    private final ListPermissionsUseCase listUseCase;
    private final ListPermissionsByCompanyUseCase listByCompanyUseCase;
    private final DeletePermissionUseCase deleteUseCase;
    private final ReactivatePermissionUseCase reactivateUseCase;
    private final Authz authz;

    public PermissionController(CreatePermissionUseCase createUseCase,
                                 UpdatePermissionUseCase updateUseCase,
                                 FindPermissionUseCase findUseCase,
                                 ListPermissionsUseCase listUseCase,
                                 ListPermissionsByCompanyUseCase listByCompanyUseCase,
                                 DeletePermissionUseCase deleteUseCase,
                                 ReactivatePermissionUseCase reactivateUseCase,
                                 Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByCompanyUseCase = listByCompanyUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PermissionResponse create(@Valid @RequestBody CreatePermissionRequest request) {
        return toResponse(createUseCase.execute(
            new CreatePermissionCommand(request.name(), request.code(), request.companyId(), request.subModuleId())));
    }

    @GetMapping
    public List<PermissionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-company")
    public List<PermissionResponse> listByCompany() {
        return listByCompanyUseCase.listByCompany(authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PermissionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public PermissionResponse update(@PathVariable Long id,
                                      @Valid @RequestBody UpdatePermissionRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdatePermissionCommand(id, request.name(), request.code(), request.companyId(), request.subModuleId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public PermissionResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private PermissionResponse toResponse(PermissionDto dto) {
        CompanySummaryDto c = dto.company();
        SubModuleSummaryDto sm = dto.subModule();
        return new PermissionResponse(
            dto.id(), dto.name(), dto.code(),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            new SubModuleSummary(sm.id(), sm.name(), sm.code()),
            dto.createdDate(),
            dto.enabled()
        );
    }
}
