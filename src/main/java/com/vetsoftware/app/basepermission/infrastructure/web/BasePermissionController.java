package com.vetsoftware.app.basepermission.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.basepermission.application.command.CreateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.command.UpdateBasePermissionCommand;
import com.vetsoftware.app.basepermission.application.dto.BasePermissionDto;
import com.vetsoftware.app.basepermission.application.port.in.CreateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.DeleteBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.FindBasePermissionUseCase;
import com.vetsoftware.app.basepermission.application.port.in.ListBasePermissionsUseCase;
import com.vetsoftware.app.basepermission.application.port.in.UpdateBasePermissionUseCase;
import com.vetsoftware.app.basepermission.infrastructure.web.request.CreateBasePermissionRequest;
import com.vetsoftware.app.basepermission.infrastructure.web.request.UpdateBasePermissionRequest;
import com.vetsoftware.app.basepermission.infrastructure.web.response.BasePermissionResponse;
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

    public BasePermissionController(CreateBasePermissionUseCase createUseCase,
                                    UpdateBasePermissionUseCase updateUseCase,
                                    FindBasePermissionUseCase findUseCase,
                                    ListBasePermissionsUseCase listUseCase,
                                    DeleteBasePermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BasePermissionResponse create(@Valid @RequestBody CreateBasePermissionRequest request,
                                         @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateBasePermissionCommand(request.name(), request.code(), request.subModuleId()), authContext));
    }

    @GetMapping
    public List<BasePermissionResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BasePermissionResponse findById(@PathVariable Long id,
                                           @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public BasePermissionResponse update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateBasePermissionRequest request,
                                         @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateBasePermissionCommand(id, request.name(), request.code(), request.subModuleId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private BasePermissionResponse toResponse(BasePermissionDto dto) {
        return new BasePermissionResponse(dto.id(), dto.name(), dto.code(), dto.subModuleId(), dto.createdDate());
    }
}
