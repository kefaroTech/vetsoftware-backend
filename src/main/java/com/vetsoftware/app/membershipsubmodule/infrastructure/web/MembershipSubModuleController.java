package com.vetsoftware.app.membershipsubmodule.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.membershipsubmodule.application.command.CreateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.command.UpdateMembershipSubModuleCommand;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSubModuleDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.MembershipSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.dto.SubModuleSummaryDto;
import com.vetsoftware.app.membershipsubmodule.application.port.in.CreateMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.DeleteMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.FindMembershipSubModuleUseCase;
import com.vetsoftware.app.membershipsubmodule.application.port.in.ListMembershipSubModulesUseCase;
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

    public MembershipSubModuleController(CreateMembershipSubModuleUseCase createUseCase,
                                          UpdateMembershipSubModuleUseCase updateUseCase,
                                          FindMembershipSubModuleUseCase findUseCase,
                                          ListMembershipSubModulesUseCase listUseCase,
                                          DeleteMembershipSubModuleUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MembershipSubModuleResponse create(@Valid @RequestBody CreateMembershipSubModuleRequest request,
                                               @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(
            new CreateMembershipSubModuleCommand(request.membershipId(), request.subModuleId()), authContext));
    }

    @GetMapping
    public List<MembershipSubModuleResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public MembershipSubModuleResponse findById(@PathVariable Long id,
                                                 @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public MembershipSubModuleResponse update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateMembershipSubModuleRequest request,
                                               @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(
            new UpdateMembershipSubModuleCommand(id, request.membershipId(), request.subModuleId()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private MembershipSubModuleResponse toResponse(MembershipSubModuleDto dto) {
        MembershipSummaryDto m = dto.membership();
        SubModuleSummaryDto sm = dto.subModule();
        return new MembershipSubModuleResponse(
            dto.id(),
            new MembershipSummary(m.id(), m.name()),
            new SubModuleSummary(sm.id(), sm.name(), sm.code()),
            dto.createdDate()
        );
    }
}
