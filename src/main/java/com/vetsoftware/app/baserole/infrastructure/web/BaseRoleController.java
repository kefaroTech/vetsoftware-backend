package com.vetsoftware.app.baserole.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.CreateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.DeleteBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.FindBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.ListBaseRolesUseCase;
import com.vetsoftware.app.baserole.application.port.in.UpdateBaseRoleUseCase;
import com.vetsoftware.app.baserole.infrastructure.web.request.CreateBaseRoleRequest;
import com.vetsoftware.app.baserole.infrastructure.web.request.UpdateBaseRoleRequest;
import com.vetsoftware.app.baserole.infrastructure.web.response.BaseRoleResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/base-roles")
public class BaseRoleController {
    private final CreateBaseRoleUseCase createUseCase;
    private final UpdateBaseRoleUseCase updateUseCase;
    private final FindBaseRoleUseCase findUseCase;
    private final ListBaseRolesUseCase listUseCase;
    private final DeleteBaseRoleUseCase deleteUseCase;

    public BaseRoleController(CreateBaseRoleUseCase createUseCase,
                               UpdateBaseRoleUseCase updateUseCase,
                               FindBaseRoleUseCase findUseCase,
                               ListBaseRolesUseCase listUseCase,
                               DeleteBaseRoleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseRoleResponse create(@RequestBody CreateBaseRoleRequest request,
                                    @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateBaseRoleCommand(request.name(), request.code(), request.mandatory()), authContext));
    }

    @GetMapping
    public List<BaseRoleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BaseRoleResponse findById(@PathVariable Long id,
                                      @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public BaseRoleResponse update(@PathVariable Long id,
                                    @RequestBody UpdateBaseRoleRequest request,
                                    @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateBaseRoleCommand(id, request.name(), request.code(), request.mandatory()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private BaseRoleResponse toResponse(BaseRoleDto dto) {
        return new BaseRoleResponse(dto.id(), dto.name(), dto.code(), dto.mandatory(), dto.createdDate());
    }
}
