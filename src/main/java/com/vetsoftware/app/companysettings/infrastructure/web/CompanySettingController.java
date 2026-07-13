package com.vetsoftware.app.companysettings.infrastructure.web;

import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.companysettings.application.command.SetCompanySettingCommand;
import com.vetsoftware.app.companysettings.application.dto.CompanySettingDto;
import com.vetsoftware.app.companysettings.application.port.in.ListCompanySettingsUseCase;
import com.vetsoftware.app.companysettings.application.port.in.SetCompanySettingUseCase;
import com.vetsoftware.app.companysettings.infrastructure.web.request.SetCompanySettingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Ajustes por empresa (clave-valor). Solo el admin (admin.all) los ve/togglea (gate en los @PreAuthorize). */
@RestController
@RequestMapping("/company-settings")
public class CompanySettingController {

    private final ListCompanySettingsUseCase listUseCase;
    private final SetCompanySettingUseCase setUseCase;
    private final Authz authz;

    public CompanySettingController(ListCompanySettingsUseCase listUseCase, SetCompanySettingUseCase setUseCase,
                                    Authz authz) {
        this.listUseCase = listUseCase;
        this.setUseCase = setUseCase;
        this.authz = authz;
    }

    @GetMapping
    public List<CompanySettingDto> list() {
        return listUseCase.listByCompany(authz.currentCompanyId());
    }

    @PutMapping
    public CompanySettingDto set(@Valid @RequestBody SetCompanySettingRequest request) {
        return setUseCase.set(new SetCompanySettingCommand(
            authz.currentCompanyId(), request.propertyName(), request.value()));
    }
}
