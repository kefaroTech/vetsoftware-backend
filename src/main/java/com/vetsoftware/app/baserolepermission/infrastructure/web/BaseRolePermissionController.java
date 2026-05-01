package com.vetsoftware.app.baserolepermission.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserolepermission.application.command.CreateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.command.UpdateBaseRolePermissionCommand;
import com.vetsoftware.app.baserolepermission.application.dto.BaseRolePermissionDto;
import com.vetsoftware.app.baserolepermission.application.port.in.CreateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.DeleteBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.FindBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.ListBaseRolePermissionsUseCase;
import com.vetsoftware.app.baserolepermission.application.port.in.UpdateBaseRolePermissionUseCase;
import com.vetsoftware.app.baserolepermission.infrastructure.web.request.CreateBaseRolePermissionRequest;
import com.vetsoftware.app.baserolepermission.infrastructure.web.request.UpdateBaseRolePermissionRequest;
import com.vetsoftware.app.baserolepermission.infrastructure.web.response.BaseRolePermissionResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/base-role-permissions")
public class BaseRolePermissionController {
    private final CreateBaseRolePermissionUseCase createUseCase;
    private final UpdateBaseRolePermissionUseCase updateUseCase;
    private final FindBaseRolePermissionUseCase findUseCase;
    private final ListBaseRolePermissionsUseCase listUseCase;
    private final DeleteBaseRolePermissionUseCase deleteUseCase;

    public BaseRolePermissionController(CreateBaseRolePermissionUseCase createUseCase,
                                         UpdateBaseRolePermissionUseCase updateUseCase,
                                         FindBaseRolePermissionUseCase findUseCase,
                                         ListBaseRolePermissionsUseCase listUseCase,
                                         DeleteBaseRolePermissionUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseRolePermissionResponse create(@RequestBody CreateBaseRolePermissionRequest request,
                                              @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateBaseRolePermissionCommand(request.baseRoleId(), request.basePermissionId()), authContext));
    }

    @GetMapping
    public List<BaseRolePermissionResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BaseRolePermissionResponse findById(@PathVariable Long id,
                                                @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public BaseRolePermissionResponse update(@PathVariable Long id,
                                              @RequestBody UpdateBaseRolePermissionRequest request,
                                              @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateBaseRolePermissionCommand(id, request.baseRoleId(), request.basePermissionId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private BaseRolePermissionResponse toResponse(BaseRolePermissionDto dto) {
        return new BaseRolePermissionResponse(dto.id(), dto.baseRoleId(), dto.basePermissionId(), dto.createdDate());
    }
}
