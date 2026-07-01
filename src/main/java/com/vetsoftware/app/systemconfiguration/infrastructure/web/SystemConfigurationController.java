package com.vetsoftware.app.systemconfiguration.infrastructure.web;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import com.vetsoftware.app.systemconfiguration.application.port.in.ListSystemConfigurationsUseCase;
import com.vetsoftware.app.systemconfiguration.application.port.in.SetSystemConfigurationUseCase;
import com.vetsoftware.app.systemconfiguration.infrastructure.web.request.SetSystemConfigurationRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * Configuración general del sistema (global, clave-valor; no scoped a empresa). Lectura para cualquier
 * autenticado; escritura (upsert por propertyName) solo admin/gestor. Controla ajustes como el UVT.
 */
@RestController
@RequestMapping("/system-configurations")
public class SystemConfigurationController {
    private final ListSystemConfigurationsUseCase listUseCase;
    private final SetSystemConfigurationUseCase setUseCase;

    public SystemConfigurationController(ListSystemConfigurationsUseCase listUseCase,
                                         SetSystemConfigurationUseCase setUseCase) {
        this.listUseCase = listUseCase;
        this.setUseCase = setUseCase;
    }

    @GetMapping
    public List<SystemConfigurationDto> list() {
        return listUseCase.listAll();
    }

    @PutMapping
    public SystemConfigurationDto set(@Valid @RequestBody SetSystemConfigurationRequest request) {
        return setUseCase.execute(new SetSystemConfigurationCommand(request.propertyName(), request.value()));
    }
}
