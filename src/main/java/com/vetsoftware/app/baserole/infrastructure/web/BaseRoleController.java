package com.vetsoftware.app.baserole.infrastructure.web;

import com.vetsoftware.app.baserole.application.command.CreateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.command.UpdateBaseRoleCommand;
import com.vetsoftware.app.baserole.application.dto.BaseRoleDto;
import com.vetsoftware.app.baserole.application.port.in.CreateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.DeleteBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.FindBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.ListBaseRolesUseCase;
import com.vetsoftware.app.baserole.application.port.in.ReactivateBaseRoleUseCase;
import com.vetsoftware.app.baserole.application.port.in.UpdateBaseRoleUseCase;
import com.vetsoftware.app.baserole.infrastructure.web.request.CreateBaseRoleRequest;
import com.vetsoftware.app.baserole.infrastructure.web.request.UpdateBaseRoleRequest;
import com.vetsoftware.app.baserole.infrastructure.web.response.BaseRoleResponse;
import jakarta.validation.Valid;
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
    private final ReactivateBaseRoleUseCase reactivateUseCase;

    public BaseRoleController(CreateBaseRoleUseCase createUseCase,
                               UpdateBaseRoleUseCase updateUseCase,
                               FindBaseRoleUseCase findUseCase,
                               ListBaseRolesUseCase listUseCase,
                               DeleteBaseRoleUseCase deleteUseCase,
                               ReactivateBaseRoleUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BaseRoleResponse create(@Valid @RequestBody CreateBaseRoleRequest request) {
        return toResponse(createUseCase.execute(
            new CreateBaseRoleCommand(request.name(), request.code(), request.mandatory())));
    }

    @GetMapping
    public List<BaseRoleResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public BaseRoleResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public BaseRoleResponse update(@PathVariable Long id,
                                    @Valid @RequestBody UpdateBaseRoleRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateBaseRoleCommand(id, request.name(), request.code(), request.mandatory())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public BaseRoleResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private BaseRoleResponse toResponse(BaseRoleDto dto) {
        return new BaseRoleResponse(dto.id(), dto.name(), dto.code(), dto.mandatory(), dto.createdDate(), dto.enabled());
    }
}
