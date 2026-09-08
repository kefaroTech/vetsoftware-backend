package com.vetsoftware.app.subscriptionmodule.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.subscriptionmodule.application.command.ListModuleShowcaseQuery;
import com.vetsoftware.app.subscriptionmodule.application.port.in.ListModuleShowcaseUseCase;
import com.vetsoftware.app.subscriptionmodule.infrastructure.web.response.ModuleShowcaseResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscriptions/modules")
public class SubscriptionModuleController {

    private final ListModuleShowcaseUseCase listUseCase;
    private final Authz authz;

    public SubscriptionModuleController(ListModuleShowcaseUseCase listUseCase, Authz authz) {
        this.listUseCase = listUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<ModuleShowcaseResponse> list() {
        return listUseCase
                .execute(new ListModuleShowcaseQuery(authz.currentCompanyId(),
                        authz.currentEmployeeId()))
                .stream().map(ModuleShowcaseResponse::from).toList();
    }
}
