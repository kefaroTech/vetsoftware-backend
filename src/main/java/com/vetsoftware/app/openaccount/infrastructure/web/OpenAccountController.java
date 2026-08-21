package com.vetsoftware.app.openaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.openaccount.application.command.ChangeOpenAccountStatusCommand;
import com.vetsoftware.app.openaccount.application.command.CreateOpenAccountCommand;
import com.vetsoftware.app.openaccount.application.command.SearchOpenAccountsCommand;
import com.vetsoftware.app.openaccount.application.dto.BranchSummaryDto;
import com.vetsoftware.app.openaccount.application.dto.CompanySummaryDto;
import com.vetsoftware.app.openaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountDto;
import com.vetsoftware.app.openaccount.domain.OpenAccountStatus;
import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import com.vetsoftware.app.openaccount.application.dto.OwnerSummaryDto;
import com.vetsoftware.app.openaccount.application.port.in.ChangeOpenAccountStatusUseCase;
import com.vetsoftware.app.openaccount.application.port.in.CreateOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.DeleteOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.FindOpenAccountUseCase;
import com.vetsoftware.app.openaccount.application.port.in.GetOpenAccountsSummaryUseCase;
import com.vetsoftware.app.openaccount.application.port.in.ListOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.application.port.in.SearchOpenAccountsUseCase;
import com.vetsoftware.app.openaccount.infrastructure.web.request.ChangeOpenAccountStatusRequest;
import com.vetsoftware.app.openaccount.infrastructure.web.request.CreateOpenAccountRequest;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OpenAccountBranchSummary;
import com.vetsoftware.app.openaccount.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OpenAccountEmployeeSummary;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OpenAccountResponse;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OpenAccountsSummaryResponse;
import com.vetsoftware.app.openaccount.infrastructure.web.response.OwnerSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open-accounts")
public class OpenAccountController {
    private final CreateOpenAccountUseCase createUseCase;
    private final FindOpenAccountUseCase findUseCase;
    private final ListOpenAccountsUseCase listUseCase;
    private final SearchOpenAccountsUseCase searchUseCase;
    private final GetOpenAccountsSummaryUseCase summaryUseCase;
    private final DeleteOpenAccountUseCase deleteUseCase;
    private final ChangeOpenAccountStatusUseCase changeStatusUseCase;
    private final Authz authz;

    public OpenAccountController(CreateOpenAccountUseCase createUseCase,
            FindOpenAccountUseCase findUseCase, ListOpenAccountsUseCase listUseCase,
            SearchOpenAccountsUseCase searchUseCase, GetOpenAccountsSummaryUseCase summaryUseCase,
            DeleteOpenAccountUseCase deleteUseCase,
            ChangeOpenAccountStatusUseCase changeStatusUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.searchUseCase = searchUseCase;
        this.summaryUseCase = summaryUseCase;
        this.deleteUseCase = deleteUseCase;
        this.changeStatusUseCase = changeStatusUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OpenAccountResponse create(@Valid @RequestBody CreateOpenAccountRequest request) {
        return toResponse(createUseCase.execute(new CreateOpenAccountCommand(request.ownerId(),
                authz.resolveAccessibleBranch(request.branchId()), authz.currentCompanyId(),
                authz.currentEmployeeId())));
    }

    @GetMapping
    public PageResponse<OpenAccountResponse> list(
            @RequestParam(name = "branchId", required = false) Long branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listByCompany(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId), page, pageSize), this::toResponse);
    }

    @GetMapping("/search")
    public PageResponse<OpenAccountResponse> search(@RequestParam(required = false) Long ownerId,
            @RequestParam(required = false) Boolean enabled,
            // Repetible: la pestana "Cerradas" manda status=CLOSE&status=CANCEL.
            @RequestParam(required = false) List<OpenAccountStatus> status,
            @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long branchId) {
        return PageResponse.from(searchUseCase
                .execute(new SearchOpenAccountsCommand(authz.currentCompanyId(), ownerId, enabled,
                        status, q, page, pageSize, authz.resolveAccessibleBranch(branchId))),
                this::toResponse);
    }

    /**
     * BE-06: contadores de las pestanas y saldo pendiente acumulado. Con la lista
     * paginada, el front ya no puede sumarlos sobre el array completo.
     */
    @GetMapping("/summary")
    public OpenAccountsSummaryResponse summary(
            @RequestParam(name = "branchId", required = false) Long branchId) {
        OpenAccountsSummaryDto dto = summaryUseCase.summarize(authz.currentCompanyId(),
                authz.resolveAccessibleBranch(branchId));
        return new OpenAccountsSummaryResponse(dto.openCount(), dto.closedCount(),
                dto.totalOutstanding());
    }

    @GetMapping("/{id}")
    public OpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id, authz.currentCompanyId());
    }

    @PatchMapping("/{id}/status")
    public OpenAccountResponse changeStatus(@PathVariable Long id,
            @Valid @RequestBody ChangeOpenAccountStatusRequest request) {
        return toResponse(changeStatusUseCase.execute(
                new ChangeOpenAccountStatusCommand(id, request.status(), authz.currentEmployeeId(),
                        request.reason(), authz.currentCompanyId(), request.documentType(),
                        request.finalConsumer(), request.expectedVersion())));
    }

    private OpenAccountResponse toResponse(OpenAccountDto dto) {
        OwnerSummaryDto o = dto.owner();
        CompanySummaryDto c = dto.company();
        EmployeeSummaryDto cb = dto.createdBy();
        EmployeeSummaryDto closed = dto.closedBy();
        BranchSummaryDto b = dto.branch();
        return new OpenAccountResponse(dto.id(), new OwnerSummary(o.id(), o.name(), o.document()),
                dto.totalAmount(), dto.paidAmount(), dto.outstandingAmount(),
                new CompanySummary(c.id(), c.name(), c.identifier()),
                new OpenAccountBranchSummary(b.id(), b.name(), b.code()), dto.status(),
                new OpenAccountEmployeeSummary(cb.id(), cb.name()), dto.createdDate(),
                dto.enabled(),
                closed != null ? new OpenAccountEmployeeSummary(closed.id(), closed.name()) : null,
                dto.closedAt(), dto.closeReason(), dto.reversed(), dto.reversedAt(), dto.version());
    }
}
