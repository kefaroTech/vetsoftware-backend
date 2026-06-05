package com.vetsoftware.app.debtopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.debtopenaccount.application.command.CreateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.command.UpdateDebtOpenAccountCommand;
import com.vetsoftware.app.debtopenaccount.application.dto.DebtOpenAccountDto;
import com.vetsoftware.app.debtopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.debtopenaccount.application.port.in.CreateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.DeleteDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.FindDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ListDebtOpenAccountsUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.ReactivateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.application.port.in.UpdateDebtOpenAccountUseCase;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.request.CreateDebtOpenAccountRequest;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.request.UpdateDebtOpenAccountRequest;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.DebtOpenAccountResponse;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.debtopenaccount.infrastructure.web.response.OpenAccountSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/debt-open-accounts")
public class DebtOpenAccountController {
    private final CreateDebtOpenAccountUseCase createUseCase;
    private final UpdateDebtOpenAccountUseCase updateUseCase;
    private final FindDebtOpenAccountUseCase findUseCase;
    private final ListDebtOpenAccountsUseCase listUseCase;
    private final ListDebtOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final DeleteDebtOpenAccountUseCase deleteUseCase;
    private final ReactivateDebtOpenAccountUseCase reactivateUseCase;
    private final Authz authz;

    public DebtOpenAccountController(CreateDebtOpenAccountUseCase createUseCase,
                                     UpdateDebtOpenAccountUseCase updateUseCase,
                                     FindDebtOpenAccountUseCase findUseCase,
                                     ListDebtOpenAccountsUseCase listUseCase,
                                     ListDebtOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
                                     DeleteDebtOpenAccountUseCase deleteUseCase,
                                     ReactivateDebtOpenAccountUseCase reactivateUseCase,
                                     Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DebtOpenAccountResponse create(@Valid @RequestBody CreateDebtOpenAccountRequest request) {
        return toResponse(createUseCase.execute(
            new CreateDebtOpenAccountCommand(
                request.amount(), request.paymentMethod(), request.openAccountId(),
                authz.currentCompanyId(), authz.currentEmployeeId())));
    }

    @GetMapping
    public List<DebtOpenAccountResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<DebtOpenAccountResponse> listByOpenAccount(@PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
            .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DebtOpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public DebtOpenAccountResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdateDebtOpenAccountRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateDebtOpenAccountCommand(
                id, request.amount(), request.paymentMethod(), request.openAccountId(),
                authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public DebtOpenAccountResponse reactivate(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private DebtOpenAccountResponse toResponse(DebtOpenAccountDto dto) {
        OpenAccountSummaryDto oa = dto.openAccount();
        EmployeeSummaryDto e = dto.createdBy();
        return new DebtOpenAccountResponse(
            dto.id(), dto.amount(), dto.paymentMethod(),
            new OpenAccountSummary(oa.id(), oa.companyId()),
            e == null ? null : new EmployeeSummary(e.id(), e.name()),
            dto.createdDate(), dto.enabled());
    }
}
