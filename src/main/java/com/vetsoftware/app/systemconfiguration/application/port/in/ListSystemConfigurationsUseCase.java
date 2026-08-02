package com.vetsoftware.app.systemconfiguration.application.port.in;

import com.vetsoftware.app.systemconfiguration.application.dto.SystemConfigurationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Lista todas las configuraciones del sistema (clave-valor). La puede leer
 * cualquier usuario autenticado: el front necesita, por ejemplo, el UVT para
 * calcular el umbral de Factura electrónica.
 */
public interface ListSystemConfigurationsUseCase {
    @PreAuthorize("isAuthenticated()")
    List<SystemConfigurationDto> listAll();
}
