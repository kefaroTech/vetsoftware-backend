package com.vetsoftware.app.systemconfiguration.application.port.in;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Crea o actualiza (upsert por {@code propertyName}) una configuración general del sistema. Es global
 * (no por empresa), así que solo lo gestiona un administrador. NOTA: `systemConfiguration.manage` aún
 * no está en el catálogo de permisos; hasta que se agregue, solo `admin.all` puede escribir.
 */
public interface SetSystemConfigurationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('systemConfiguration.manage')")
    SystemConfigurationDto execute(SetSystemConfigurationCommand command);
}
