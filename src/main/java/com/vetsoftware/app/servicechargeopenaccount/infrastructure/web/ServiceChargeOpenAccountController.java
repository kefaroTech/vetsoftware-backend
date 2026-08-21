package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.command.VoidServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.infrastructure.web.PageResponse;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.CreateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.VoidServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request.CreateServiceChargeOpenAccountRequest;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request.VoidServiceChargeOpenAccountRequest;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.ServiceChargeOpenAccountEmployeeSummary;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.OpenAccountSummary;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.ServiceChargeOpenAccountResponse;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.ServiceSummary;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/service-charge-open-accounts")
public class ServiceChargeOpenAccountController {
    private final CreateServiceChargeOpenAccountUseCase createUseCase;
    private final ListServiceChargeOpenAccountsUseCase listUseCase;
    private final ListServiceChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final VoidServiceChargeOpenAccountUseCase voidUseCase;
    private final Authz authz;

    public ServiceChargeOpenAccountController(CreateServiceChargeOpenAccountUseCase createUseCase,
            ListServiceChargeOpenAccountsUseCase listUseCase,
            ListServiceChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
            VoidServiceChargeOpenAccountUseCase voidUseCase, Authz authz) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.listByOpenAccountUseCase = listByOpenAccountUseCase;
        this.voidUseCase = voidUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceChargeOpenAccountResponse create(
            @Valid @RequestBody CreateServiceChargeOpenAccountRequest request) {
        return toResponse(createUseCase.execute(new CreateServiceChargeOpenAccountCommand(
                request.animalId(), request.serviceId(), request.openAccountId(),
                authz.currentCompanyId(), authz.currentEmployeeId(), request.clientRequestId(),
                request.expectedVersion())));
    }

    @GetMapping
    public PageResponse<ServiceChargeOpenAccountResponse> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return PageResponse.from(listUseCase.listAll(authz.currentCompanyId(), page, pageSize),
                this::toResponse);
    }

    @GetMapping("/by-open-account/{openAccountId}")
    public List<ServiceChargeOpenAccountResponse> listByOpenAccount(
            @PathVariable Long openAccountId) {
        return listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
                .stream().map(this::toResponse).toList();
    }

    @PatchMapping("/{id}/void")
    public ServiceChargeOpenAccountResponse voidCharge(@PathVariable Long id,
            @Valid @RequestBody VoidServiceChargeOpenAccountRequest request) {
        return toResponse(voidUseCase
                .execute(new VoidServiceChargeOpenAccountCommand(id, authz.currentCompanyId(),
                        authz.currentEmployeeId(), request.reason(), request.expectedVersion())));
    }

    private ServiceChargeOpenAccountResponse toResponse(ServiceChargeOpenAccountDto dto) {
        AnimalSummaryDto a = dto.animal();
        ServiceSummaryDto s = dto.service();
        OpenAccountSummaryDto o = dto.openAccount();
        EmployeeSummaryDto e = dto.createdBy();
        EmployeeSummaryDto v = dto.voidedBy();
        return new ServiceChargeOpenAccountResponse(dto.id(),
                new AnimalSummary(a.id(), a.name(), a.code()),
                new ServiceSummary(s.id(), s.name(), s.price()), dto.unitPrice(), dto.hasTax(),
                dto.taxPercentage(), dto.taxName(), dto.baseAmount(), dto.taxAmount(),
                dto.totalAmount(), new OpenAccountSummary(o.id(), o.companyId()),
                e == null ? null : new ServiceChargeOpenAccountEmployeeSummary(e.id(), e.name()),
                dto.createdDate(), dto.enabled(), dto.voided(),
                v == null ? null : new ServiceChargeOpenAccountEmployeeSummary(v.id(), v.name()),
                dto.voidedAt(), dto.voidReason());
    }
}
