package com.vetsoftware.app.debtopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.command.VoidDebtOpenAccountCommand;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.CreateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.VoidDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.request.CreateDebtOpenAccountRequest;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.request.VoidDebtOpenAccountRequest;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.DebtOpenAccountResponse;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.DebtOpenAccountEmployeeSummary;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.OpenAccountSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debt-open-accounts")
public class DebtOpenAccountController {
    private final CreateDebtOpenAccountUseCase createUseCase;
    private final ListDebtOpenAccountsUseCase listUseCase;
    private final ListDebtOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final VoidDebtOpenAccountUseCase voidUseCase;
    private final Authz authz;

    public DebtOpenAccountController(CreateDebtOpenAccountUseCase createUseCase,
            ListDebtOpenAccountsUseCase listUseCase,
            ListDebtOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
            VoidDebtOpenAccountUseCase voidUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
        this.voidUseCase = voidUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebtOpenAccountResponse create(
            @Valid @RequestBody CreateDebtOpenAccountRequest request) {
        return toResponse(createUseCase.execute(new CreateDebtOpenAccountCommand(request.amount(),
                request.paymentMethod(), request.openAccountId(), authz.currentCompanyId(),
                authz.currentEmployeeId(), request.clientRequestId(), request.expectedVersion())));
    }

    @GetMapping
    public PageResponse<DebtOpenAccountResponse> listAll(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<DebtOpenAccountResponse> listByOpenAccount(@PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/void")
    public DebtOpenAccountResponse voidPayment(@PathVariable Long id,
            @Valid @RequestBody VoidDebtOpenAccountRequest request) {
        return toResponse(
                voidUseCase.execute(new VoidDebtOpenAccountCommand(id, authz.currentCompanyId(),
                        authz.currentEmployeeId(), request.reason(), request.expectedVersion())));
    }

    private DebtOpenAccountResponse toResponse(DebtOpenAccountDto dto) {
        OpenAccountSummaryDto oa = dto.openAccount();
        EmployeeSummaryDto e = dto.createdBy();
        EmployeeSummaryDto v = dto.voidedBy();
        return new DebtOpenAccountResponse(dto.id(), dto.amount(), dto.paymentMethod(),
                new OpenAccountSummary(oa.id(), oa.companyId()),
                e == null ? null : new DebtOpenAccountEmployeeSummary(e.id(), e.name()),
                dto.createdDate(), dto.enabled(), dto.voided(),
                v == null ? null : new DebtOpenAccountEmployeeSummary(v.id(), v.name()),
                dto.voidedAt(), dto.voidReason());
    }
}
