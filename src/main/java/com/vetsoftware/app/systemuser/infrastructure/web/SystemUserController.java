package com.vetsoftware.app.systemuser.infrastructure.web;

import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.CreateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.DeleteSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.FindSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.ListSystemUsersUseCase;
import com.vetsoftware.app.systemuser.application.port.in.ReactivateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.UpdateSystemUserUseCase;
import com.vetsoftware.app.systemuser.infrastructure.web.request.CreateSystemUserRequest;
import com.vetsoftware.app.systemuser.infrastructure.web.request.UpdateSystemUserRequest;
import com.vetsoftware.app.systemuser.infrastructure.web.response.SystemUserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system-users")
public class SystemUserController {
    private final CreateSystemUserUseCase createUseCase;
    private final UpdateSystemUserUseCase updateUseCase;
    private final FindSystemUserUseCase findUseCase;
    private final ListSystemUsersUseCase listUseCase;
    private final DeleteSystemUserUseCase deleteUseCase;
    private final ReactivateSystemUserUseCase reactivateUseCase;

    public SystemUserController(CreateSystemUserUseCase createUseCase,
                                UpdateSystemUserUseCase updateUseCase,
                                FindSystemUserUseCase findUseCase,
                                ListSystemUsersUseCase listUseCase,
                                DeleteSystemUserUseCase deleteUseCase,
                                ReactivateSystemUserUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemUserResponse create(@Valid @RequestBody CreateSystemUserRequest request) {
        return toResponse(createUseCase.execute(
            new CreateSystemUserCommand(request.code(), request.password())));
    }

    @GetMapping
    public List<SystemUserResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SystemUserResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public SystemUserResponse update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateSystemUserRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateSystemUserCommand(id, request.code())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public SystemUserResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private SystemUserResponse toResponse(SystemUserDto dto) {
        return new SystemUserResponse(dto.id(), dto.code(), dto.createdDate(), dto.enabled());
    }
}
