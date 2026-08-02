package com.vetsoftware.app.membership.infrastructure.web;

import com.vetsoftware.app.membership.application.command.CreateMembershipCommand;
import com.vetsoftware.app.membership.application.command.UpdateMembershipCommand;
import com.vetsoftware.app.membership.application.dto.MembershipDto;
import com.vetsoftware.app.membership.application.port.in.CreateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.DeleteMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.FindMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.ListMembershipsUseCase;
import com.vetsoftware.app.membership.application.port.in.ReactivateMembershipUseCase;
import com.vetsoftware.app.membership.application.port.in.UpdateMembershipUseCase;
import com.vetsoftware.app.membership.infrastructure.web.request.CreateMembershipRequest;
import com.vetsoftware.app.membership.infrastructure.web.request.UpdateMembershipRequest;
import com.vetsoftware.app.membership.infrastructure.web.response.MembershipResponse;
import jakarta.validation.Valid;
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
    private final ReactivateMembershipUseCase reactivateUseCase;

    public MembershipController(CreateMembershipUseCase createUseCase,
            UpdateMembershipUseCase updateUseCase, FindMembershipUseCase findUseCase,
            ListMembershipsUseCase listUseCase, DeleteMembershipUseCase deleteUseCase,
            ReactivateMembershipUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipResponse create(@Valid @RequestBody CreateMembershipRequest request) {
        return toResponse(createUseCase.execute(new CreateMembershipCommand(request.name(),
                request.status(), request.mandatory())));
    }

    @GetMapping
    public List<MembershipResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MembershipResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public MembershipResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateMembershipRequest request) {
        return toResponse(updateUseCase.execute(new UpdateMembershipCommand(id, request.name(),
                request.status(), request.mandatory())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public MembershipResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private MembershipResponse toResponse(MembershipDto dto) {
        return new MembershipResponse(dto.id(), dto.name(), dto.status(), dto.mandatory(),
                dto.createdDate(), dto.enabled());
    }
}
