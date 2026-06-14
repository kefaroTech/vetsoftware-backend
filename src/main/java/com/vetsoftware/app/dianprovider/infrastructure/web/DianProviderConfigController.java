package com.vetsoftware.app.dianprovider.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.dianprovider.application.command.CreateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.command.UpdateDianProviderConfigCommand;
import com.vetsoftware.app.dianprovider.application.dto.DianProviderConfigDto;
import com.vetsoftware.app.dianprovider.application.port.in.CreateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.in.FindDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.application.port.in.UpdateDianProviderConfigUseCase;
import com.vetsoftware.app.dianprovider.infrastructure.web.request.CreateDianProviderConfigRequest;
import com.vetsoftware.app.dianprovider.infrastructure.web.request.UpdateDianProviderConfigRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dian-provider-configs")
public class DianProviderConfigController {
    private final CreateDianProviderConfigUseCase createUseCase;
    private final UpdateDianProviderConfigUseCase updateUseCase;
    private final FindDianProviderConfigUseCase findUseCase;
    private final Authz authz;

    public DianProviderConfigController(CreateDianProviderConfigUseCase createUseCase,
                                        UpdateDianProviderConfigUseCase updateUseCase,
                                        FindDianProviderConfigUseCase findUseCase,
                                        Authz authz) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.authz = authz;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DianProviderConfigDto create(@Valid @RequestBody CreateDianProviderConfigRequest request) {
        return createUseCase.execute(new CreateDianProviderConfigCommand(
                request.provider(), request.environment(), request.baseUrl(), request.clientId(),
                request.clientSecret(), request.username(), request.password(), request.apiToken(),
                request.webhookSecret(), request.numberingProviderRef(), authz.currentCompanyId()));
    }

    @PutMapping
    public DianProviderConfigDto update(@Valid @RequestBody UpdateDianProviderConfigRequest request) {
        return updateUseCase.execute(new UpdateDianProviderConfigCommand(
                request.provider(), request.environment(), request.baseUrl(), request.clientId(),
                request.clientSecret(), request.username(), request.password(), request.apiToken(),
                request.webhookSecret(), request.numberingProviderRef(), authz.currentCompanyId()));
    }

    @GetMapping
    public DianProviderConfigDto find() {
        return findUseCase.findByCompany(authz.currentCompanyId());
    }
}
