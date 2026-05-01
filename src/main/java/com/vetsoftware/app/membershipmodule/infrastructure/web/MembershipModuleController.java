package com.vetsoftware.app.membershipmodule.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipmodule.application.command.CreateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.command.UpdateMembershipModuleCommand;
import com.vetsoftware.app.membershipmodule.application.dto.MembershipModuleDto;
import com.vetsoftware.app.membershipmodule.application.port.in.CreateMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.in.DeleteMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.in.FindMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.application.port.in.ListMembershipModulesUseCase;
import com.vetsoftware.app.membershipmodule.application.port.in.UpdateMembershipModuleUseCase;
import com.vetsoftware.app.membershipmodule.infrastructure.web.request.CreateMembershipModuleRequest;
import com.vetsoftware.app.membershipmodule.infrastructure.web.request.UpdateMembershipModuleRequest;
import com.vetsoftware.app.membershipmodule.infrastructure.web.response.MembershipModuleResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-modules")
public class MembershipModuleController {
    private final CreateMembershipModuleUseCase createUseCase;
    private final UpdateMembershipModuleUseCase updateUseCase;
    private final FindMembershipModuleUseCase findUseCase;
    private final ListMembershipModulesUseCase listUseCase;
    private final DeleteMembershipModuleUseCase deleteUseCase;

    public MembershipModuleController(CreateMembershipModuleUseCase createUseCase,
                                       UpdateMembershipModuleUseCase updateUseCase,
                                       FindMembershipModuleUseCase findUseCase,
                                       ListMembershipModulesUseCase listUseCase,
                                       DeleteMembershipModuleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipModuleResponse create(@RequestBody CreateMembershipModuleRequest request,
                                            @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateMembershipModuleCommand(request.membershipId(), request.subModuleId()), authContext));
    }

    @GetMapping
    public List<MembershipModuleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MembershipModuleResponse findById(@PathVariable Long id,
                                              @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public MembershipModuleResponse update(@PathVariable Long id,
                                            @RequestBody UpdateMembershipModuleRequest request,
                                            @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateMembershipModuleCommand(id, request.membershipId(), request.subModuleId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private MembershipModuleResponse toResponse(MembershipModuleDto dto) {
        return new MembershipModuleResponse(dto.id(), dto.membershipId(), dto.subModuleId(), dto.createdDate());
    }
}
