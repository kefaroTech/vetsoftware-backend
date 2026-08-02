package com.vetsoftware.app.membershipsubmodule.infrastructure.web;

import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.DeleteMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.FindMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ListMembershipSubModulesUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ReactivateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.UpdateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.infrastructure.web.request.CreateMembershipSubModuleRequest;
import com.vetsoftware.app.membershipsubmodule.infrastructure.web.request.UpdateMembershipSubModuleRequest;
import com.vetsoftware.app.membershipsubmodule.infrastructure.web.response.MembershipSubModuleResponse;
import com.vetsoftware.app.membershipsubmodule.infrastructure.web.response.MembershipSummary;
import com.vetsoftware.app.membershipsubmodule.infrastructure.web.response.SubModuleSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-sub-modules")
public class MembershipSubModuleController {
    private final CreateMembershipSubModuleUseCase createUseCase;
    private final UpdateMembershipSubModuleUseCase updateUseCase;
    private final FindMembershipSubModuleUseCase findUseCase;
    private final ListMembershipSubModulesUseCase listUseCase;
    private final DeleteMembershipSubModuleUseCase deleteUseCase;
    private final ReactivateMembershipSubModuleUseCase reactivateUseCase;

    public MembershipSubModuleController(CreateMembershipSubModuleUseCase createUseCase,
            UpdateMembershipSubModuleUseCase updateUseCase,
            FindMembershipSubModuleUseCase findUseCase, ListMembershipSubModulesUseCase listUseCase,
            DeleteMembershipSubModuleUseCase deleteUseCase,
            ReactivateMembershipSubModuleUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipSubModuleResponse create(
            @Valid @RequestBody CreateMembershipSubModuleRequest request) {
        return toResponse(createUseCase.execute(new CreateMembershipSubModuleCommand(
                request.membershipId(), request.subModuleId())));
    }

    @GetMapping
    public List<MembershipSubModuleResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MembershipSubModuleResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public MembershipSubModuleResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateMembershipSubModuleRequest request) {
        return toResponse(updateUseCase.execute(new UpdateMembershipSubModuleCommand(id,
                request.membershipId(), request.subModuleId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public MembershipSubModuleResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private MembershipSubModuleResponse toResponse(MembershipSubModuleDto dto) {
        MembershipSummaryDto m = dto.membership();
        SubModuleSummaryDto sm = dto.subModule();
        return new MembershipSubModuleResponse(dto.id(), new MembershipSummary(m.id(), m.name()),
                new SubModuleSummary(sm.id(), sm.name(), sm.code()), dto.createdDate(),
                dto.enabled());
    }
}
