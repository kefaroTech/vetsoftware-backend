package com.vetsoftware.app.basepermission.infrastructure.web;

import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.basepermission.application.port.in.CreateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.DeleteBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.FindBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.ListBasePermissionsUseCase;
import com.vetsoftware.app.basepermission.application.port.in.ReactivateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.UpdateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.infrastructure.web.request.CreateBasePermissionRequest;
import com.vetsoftware.app.basepermission.infrastructure.web.request.UpdateBasePermissionRequest;
import com.vetsoftware.app.basepermission.infrastructure.web.response.BasePermissionResponse;
import com.vetsoftware.app.basepermission.infrastructure.web.response.SubModuleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/base-permissions")
public class BasePermissionController {
    private final CreateBasePermissionUseCase createUseCase;
    private final UpdateBasePermissionUseCase updateUseCase;
    private final FindBasePermissionUseCase findUseCase;
    private final ListBasePermissionsUseCase listUseCase;
    private final DeleteBasePermissionUseCase deleteUseCase;
    private final ReactivateBasePermissionUseCase reactivateUseCase;

    public BasePermissionController(CreateBasePermissionUseCase createUseCase,
                                    UpdateBasePermissionUseCase updateUseCase,
                                    FindBasePermissionUseCase findUseCase,
                                    ListBasePermissionsUseCase listUseCase,
                                    DeleteBasePermissionUseCase deleteUseCase,
                                    ReactivateBasePermissionUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BasePermissionResponse create(@Valid @RequestBody CreateBasePermissionRequest request) {
        return toResponse(createUseCase.execute(
            new CreateBasePermissionCommand(request.name(), request.code(), request.subModuleId())));
    }

    @GetMapping
    public List<BasePermissionResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BasePermissionResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public BasePermissionResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateBasePermissionRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateBasePermissionCommand(id, request.name(), request.code(), request.subModuleId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public BasePermissionResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private BasePermissionResponse toResponse(BasePermissionDto dto) {
        SubModuleSummaryDto sm = dto.subModule();
        return new BasePermissionResponse(
            dto.id(), dto.name(), dto.code(),
            new SubModuleSummary(sm.id(), sm.name(), sm.code()),
            dto.createdDate(),
            dto.enabled()
        );
    }
}
