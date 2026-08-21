package com.vetsoftware.app.generalchargeopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.generalchargeopenaccount.application.command.CreateGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.command.VoidGeneralChargeOpenAccountCommand;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.GeneralChargeOpenAccountDto;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.TaxSummaryDto;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.CreateGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.ListGeneralChargeOpenAccountsUseCase;
import com.vetsoftware.app.generalchargeopenaccount.application.port.in.VoidGeneralChargeOpenAccountUseCase;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request.CreateGeneralChargeOpenAccountRequest;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.request.VoidGeneralChargeOpenAccountRequest;
import com.vetsoftware.app.generalchargeopenaccount.infrastructure.web.response.GeneralChargeOpenAccountEmployeeSummary;
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
    private final ListGeneralChargeOpenAccountsUseCase listUseCase;
    private final ListGeneralChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final VoidGeneralChargeOpenAccountUseCase voidUseCase;
    private final Authz authz;

    public GeneralChargeOpenAccountController(CreateGeneralChargeOpenAccountUseCase createUseCase,
            ListGeneralChargeOpenAccountsUseCase listUseCase,
            ListGeneralChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
            VoidGeneralChargeOpenAccountUseCase voidUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
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
        return PageResponse.from(listUseCase.listAll(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<GeneralChargeOpenAccountResponse> listByOpenAccount(
            @PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
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
                new GeneralChargeOpenAccountEmployeeSummary(emp.id(), emp.name()),
                dto.createdDate(), dto.enabled(), dto.voided(),
                v == null ? null : new GeneralChargeOpenAccountEmployeeSummary(v.id(), v.name()),
                dto.voidedAt(), dto.voidReason());
    }
}
