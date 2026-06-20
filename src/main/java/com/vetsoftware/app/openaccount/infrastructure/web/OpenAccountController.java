package com.vetsoftware.app.openaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.command.UpdateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.dto.CompanySummaryDto;
import com.vetsoftware.app.openaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.application.dto.OwnerSummaryDto;
import com.vetsoftware.app.openaccount.application.dto.PageResult;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.DeleteOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.FindOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.ListOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.in.ReactivateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.SearchOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.in.UpdateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.infrastructure.web.request.ChangeOpenAccountStatusRequest;
import com.vetsoftware.app.openaccount.infrastructure.web.request.CreateOpenAccountRequest;
import com.vetsoftware.app.openaccount.infrastructure.web.request.UpdateOpenAccountRequest;
import com.vetsoftware.app.openaccount.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.openaccount.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OpenAccountResponse;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OwnerSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-accounts")
public class OpenAccountController {
    private final CreateOpenAccountUseCase createUseCase;
    private final UpdateOpenAccountUseCase updateUseCase;
    private final FindOpenAccountUseCase findUseCase;
    private final ListOpenAccountsUseCase listUseCase;
    private final SearchOpenAccountsUseCase searchUseCase;
    private final DeleteOpenAccountUseCase deleteUseCase;
    private final ReactivateOpenAccountUseCase reactivateUseCase;
    private final ChangeOpenAccountStatusUseCase changeStatusUseCase;
    private final Authz authz;

    public OpenAccountController(CreateOpenAccountUseCase createUseCase,
                                UpdateOpenAccountUseCase updateUseCase,
                                FindOpenAccountUseCase findUseCase,
                                ListOpenAccountsUseCase listUseCase,
                                SearchOpenAccountsUseCase searchUseCase,
                                DeleteOpenAccountUseCase deleteUseCase,
                                ReactivateOpenAccountUseCase reactivateUseCase,
                                ChangeOpenAccountStatusUseCase changeStatusUseCase,
                                Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenAccountResponse create(@Valid @RequestBody CreateOpenAccountRequest request) {
        return toResponse(createUseCase.execute(
            new CreateOpenAccountCommand(
                request.ownerId(), authz.currentCompanyId(), authz.currentEmployeeId())));
    }

    @GetMapping
    public List<OpenAccountResponse> list() {
        return listUseCase.listByCompany(authz.currentCompanyId()).stream().map(this::toResponse).toList();
    }

    @GetMapping("/search")
    public PageResponse<OpenAccountResponse> search(
            @RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<OpenAccountDto> result = searchUseCase.execute(new SearchOpenAccountsCommand(
            authz.currentCompanyId(), ownerId, enabled, page, pageSize));
        return new PageResponse<>(
            result.content().stream().map(this::toResponse).toList(),
            result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/{id}")
    public OpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public OpenAccountResponse update(@PathVariable Long id, @Valid @RequestBody UpdateOpenAccountRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateOpenAccountCommand(id, request.ownerId(), authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/enable")
    public OpenAccountResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    @PatchMapping("/{id}/status")
    public OpenAccountResponse changeStatus(@PathVariable Long id,
                                            @Valid @RequestBody ChangeOpenAccountStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(
            new ChangeOpenAccountStatusCommand(id, request.status(), authz.currentEmployeeId(),
                request.reason(), authz.currentCompanyId(),
                request.documentType(), request.finalConsumer())));
    }

    private OpenAccountResponse toResponse(OpenAccountDto dto) {
        OwnerSummaryDto o = dto.owner();
        CompanySummaryDto c = dto.company();
        EmployeeSummaryDto cb = dto.createdBy();
        EmployeeSummaryDto closed = dto.closedBy();
        return new OpenAccountResponse(
            dto.id(),
            new OwnerSummary(o.id(), o.name(), o.document()),
            dto.totalAmount(), dto.paidAmount(), dto.outstandingAmount(),
            new CompanySummary(c.id(), c.name(), c.identifier()),
            dto.status(),
            new EmployeeSummary(cb.id(), cb.name()),
            dto.createdDate(), dto.enabled(),
            closed != null ? new EmployeeSummary(closed.id(), closed.name()) : null,
            dto.closedAt(), dto.closeReason(),
            dto.reversed(), dto.reversedAt());
    }
}
