package com.vetsoftware.app.servicechargeopenaccount.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.servicechargeopenaccount.application.command.CreateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.command.UpdateServiceChargeOpenAccountCommand;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.AnimalSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.EmployeeSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.OpenAccountSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceChargeOpenAccountDto;
import com.vetsoftware.app.servicechargeopenaccount.application.dto.ServiceSummaryDto;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.CreateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.DeleteServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.FindServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsByOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ListServiceChargeOpenAccountsUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.ReactivateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.application.port.in.UpdateServiceChargeOpenAccountUseCase;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request.CreateServiceChargeOpenAccountRequest;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.request.UpdateServiceChargeOpenAccountRequest;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.AnimalSummary;
import com.vetsoftware.app.servicechargeopenaccount.infrastructure.web.response.EmployeeSummary;
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
    private final UpdateServiceChargeOpenAccountUseCase updateUseCase;
    private final FindServiceChargeOpenAccountUseCase findUseCase;
    private final ListServiceChargeOpenAccountsUseCase listUseCase;
    private final ListServiceChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase;
    private final DeleteServiceChargeOpenAccountUseCase deleteUseCase;
    private final ReactivateServiceChargeOpenAccountUseCase reactivateUseCase;
    private final Authz authz;

    public ServiceChargeOpenAccountController(CreateServiceChargeOpenAccountUseCase createUseCase,
                                              UpdateServiceChargeOpenAccountUseCase updateUseCase,
                                              FindServiceChargeOpenAccountUseCase findUseCase,
                                              ListServiceChargeOpenAccountsUseCase listUseCase,
                                              ListServiceChargeOpenAccountsByOpenAccountUseCase listByOpenAccountUseCase,
                                              DeleteServiceChargeOpenAccountUseCase deleteUseCase,
                                              ReactivateServiceChargeOpenAccountUseCase reactivateUseCase,
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
    public ServiceChargeOpenAccountResponse create(
            @Valid @RequestBody CreateServiceChargeOpenAccountRequest request) {
        return toResponse(createUseCase.execute(
            new CreateServiceChargeOpenAccountCommand(
                request.animalId(), request.serviceId(), request.openAccountId(),
                authz.currentCompanyId(), authz.currentEmployeeId())));
    }

    @GetMapping
    public List<ServiceChargeOpenAccountResponse> list(@RequestParam(required = false) Long openAccountId) {
        List<ServiceChargeOpenAccountDto> result = openAccountId != null
            ? listByOpenAccountUseCase.listByOpenAccount(openAccountId, authz.currentCompanyId())
            : listUseCase.listAll();
        return result.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ServiceChargeOpenAccountResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ServiceChargeOpenAccountResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateServiceChargeOpenAccountRequest request) {
        return toResponse(updateUseCase.execute(
            new UpdateServiceChargeOpenAccountCommand(
                id, request.animalId(), request.serviceId(), request.openAccountId(),
                authz.currentCompanyId())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public ServiceChargeOpenAccountResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private ServiceChargeOpenAccountResponse toResponse(ServiceChargeOpenAccountDto dto) {
        AnimalSummaryDto a = dto.animal();
        ServiceSummaryDto s = dto.service();
        OpenAccountSummaryDto o = dto.openAccount();
        EmployeeSummaryDto e = dto.createdBy();
        return new ServiceChargeOpenAccountResponse(
            dto.id(),
            new AnimalSummary(a.id(), a.name(), a.code()),
            new ServiceSummary(s.id(), s.name(), s.price()),
            new OpenAccountSummary(o.id(), o.companyId()),
            e == null ? null : new EmployeeSummary(e.id(), e.name()),
            dto.createdDate(),
            dto.enabled());
    }
}
