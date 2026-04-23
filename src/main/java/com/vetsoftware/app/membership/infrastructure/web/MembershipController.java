package com.vetsoftware.app.membership.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.CreateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.DeleteMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.FindMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.ListMembershipsUseCase;
import com.vetsoftware.app.membership.application.port.in.UpdateMembershipUseCase;
import com.vetsoftware.app.membership.infrastructure.web.request.CreateMembershipRequest;
import com.vetsoftware.app.membership.infrastructure.web.request.UpdateMembershipRequest;
import com.vetsoftware.app.membership.infrastructure.web.response.MembershipResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/memberships")
public class MembershipController {
    private final CreateMembershipUseCase createUseCase;
    private final UpdateMembershipUseCase updateUseCase;
    private final FindMembershipUseCase findUseCase;
    private final ListMembershipsUseCase listUseCase;
    private final DeleteMembershipUseCase deleteUseCase;

    public MembershipController(CreateMembershipUseCase createUseCase, UpdateMembershipUseCase updateUseCase,
                                FindMembershipUseCase findUseCase, ListMembershipsUseCase listUseCase,
                                DeleteMembershipUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@RequestBody CreateMembershipRequest request,
                                     @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateMembershipCommand(request.name(), request.status(), request.moduleIds()),
            authContext
        ));
    }

    @GetMapping
    public List<MembershipResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MembershipResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public MembershipResponse update(@PathVariable Long id, @RequestBody UpdateMembershipRequest request,
                                     @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateMembershipCommand(id, request.name(), request.status(), request.moduleIds()),
            authContext
        ));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private MembershipResponse toResponse(MembershipDto dto) {
        return new MembershipResponse(dto.id(), dto.name(), dto.status(), dto.createdDate(), dto.moduleIds());
    }
}
