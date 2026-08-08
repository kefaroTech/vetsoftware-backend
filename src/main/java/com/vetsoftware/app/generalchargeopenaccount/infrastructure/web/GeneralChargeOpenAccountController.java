package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.command.UpdateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.PageResult;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.TaxSummaryDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.CreateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.FindGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ReactivateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.UpdateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.VoidGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request.CreateGeneralChargeOpenAccountRequest;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request.UpdateGeneralChargeOpenAccountRequest;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request.VoidGeneralChargeOpenAccountRequest;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response.EmployeeSummary;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response.GeneralChargeOpenAccountResponse;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response.OpenAccountSummary;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response.TaxSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/general-charge-open-accounts")
public class GeneralChargeOpenAccountController {
    private final CreateGeneralChargeOpenAccountUseCase createUseCase;
    private final UpdateGeneralChargeOpenAccountUseCase updateUseCase;
    private final FindGeneralChargeOpenAccountUseCase findUseCase;
    private final ListGeneralChargeOpenAccountsUseCase listUseCase;
    private final ListGeneralChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final ReactivateGeneralChargeOpenAccountUseCase reactivateUseCase;
    private final VoidGeneralChargeOpenAccountUseCase voidUseCase;
    private final Authz authz;

    public GeneralChargeOpenAccountController(CreateGeneralChargeOpenAccountUseCase createUseCase,
            UpdateGeneralChargeOpenAccountUseCase updateUseCase,
            FindGeneralChargeOpenAccountUseCase findUseCase,
            ListGeneralChargeOpenAccountsUseCase listUseCase,
            ListGeneralChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
            ReactivateGeneralChargeOpenAccountUseCase reactivateUseCase,
            VoidGeneralChargeOpenAccountUseCase voidUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
        this.reactivateUseCase = reactivateUseCase;
        this.voidUseCase = voidUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GeneralChargeOpenAccountResponse create(
            @Valid @RequestBody CreateGeneralChargeOpenAccountRequest request) {
        return toResponse(createUseCase.execute(new CreateGeneralChargeOpenAccountCommand(
                request.name(), request.unitAmount(), request.quantity(), request.taxId(),
                request.openAccountId(), authz.currentCompanyId(), authz.currentEmployeeId(),
                request.clientRequestId(), request.expectedVersion())));
    }

    @GetMapping
    public PageResponse<GeneralChargeOpenAccountResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<GeneralChargeOpenAccountDto> result = listUseCase
                .listAll(authz.currentCompanyId(), page, pageSize);
        return new PageResponse<>(result.content().stream().map(this::toResponse).toList(),
                result.page(), result.pageSize(), result.totalElements(), result.totalPages());
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<GeneralChargeOpenAccountResponse> listByOpenAccount(
            @PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public GeneralChargeOpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id, authz.currentCompanyId()));
    }

    @PutMapping("/{id}")
    public GeneralChargeOpenAccountResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateGeneralChargeOpenAccountRequest request) {
        return toResponse(updateUseCase.execute(new UpdateGeneralChargeOpenAccountCommand(id,
                request.name(), request.unitAmount(), request.quantity(), request.taxId(),
                request.openAccountId(), authz.currentCompanyId(), request.expectedVersion())));
    }

    @PatchMapping("/{id}/enable")
    public GeneralChargeOpenAccountResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id, authz.currentCompanyId()));
    }

    @PatchMapping("/{id}/void")
    public GeneralChargeOpenAccountResponse voidCharge(@PathVariable Long id,
            @Valid @RequestBody VoidGeneralChargeOpenAccountRequest request) {
        return toResponse(voidUseCase
                .execute(new VoidGeneralChargeOpenAccountCommand(id, authz.currentCompanyId(),
                        authz.currentEmployeeId(), request.reason(), request.expectedVersion())));
    }

    private GeneralChargeOpenAccountResponse toResponse(GeneralChargeOpenAccountDto dto) {
        TaxSummaryDto t = dto.tax();
        OpenAccountSummaryDto oa = dto.openAccount();
        EmployeeSummaryDto emp = dto.createdBy();
        EmployeeSummaryDto v = dto.voidedBy();
        return new GeneralChargeOpenAccountResponse(dto.id(), dto.name(), dto.unitAmount(),
                dto.quantity(), t == null ? null : new TaxSummary(t.id(), t.name(), t.percentage()),
                dto.hasTax(), dto.taxPercentage(), dto.taxName(), dto.baseAmount(), dto.taxAmount(),
                dto.totalAmount(), new OpenAccountSummary(oa.id(), oa.companyId()),
                new EmployeeSummary(emp.id(), emp.name()), dto.createdDate(), dto.enabled(),
                dto.voided(), v == null ? null : new EmployeeSummary(v.id(), v.name()),
                dto.voidedAt(), dto.voidReason());
    }
}
