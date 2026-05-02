package com.vetsoftware.app.systemuser.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.systemuser.application.command.CreateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.command.UpdateSystemUserCommand;
import com.vetsoftware.app.systemuser.application.dto.SystemUserDto;
import com.vetsoftware.app.systemuser.application.port.in.CreateSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.DeleteSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.FindSystemUserUseCase;
import com.vetsoftware.app.systemuser.application.port.in.ListSystemUsersUseCase;
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

    public SystemUserController(CreateSystemUserUseCase createUseCase,
                                UpdateSystemUserUseCase updateUseCase,
                                FindSystemUserUseCase findUseCase,
                                ListSystemUsersUseCase listUseCase,
                                DeleteSystemUserUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SystemUserResponse create(@Valid @RequestBody CreateSystemUserRequest request,
                                     @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateSystemUserCommand(request.code(), request.password()), authContext));
    }

    @GetMapping
    public List<SystemUserResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public SystemUserResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public SystemUserResponse update(@PathVariable Long id,
                                     @Valid @RequestBody UpdateSystemUserRequest request,
                                     @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateSystemUserCommand(id, request.code()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private SystemUserResponse toResponse(SystemUserDto dto) {
        return new SystemUserResponse(dto.id(), dto.code(), dto.createdDate());
    }
}
