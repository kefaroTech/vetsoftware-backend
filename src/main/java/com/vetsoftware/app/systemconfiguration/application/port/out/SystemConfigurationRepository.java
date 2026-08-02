package com.vetsoftware.app.systemconfiguration.application.port.out;

import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import java.util.List;
import java.util.Optional;

public interface SystemConfigurationRepository {
    SystemConfiguration save(SystemConfiguration config);

    /**
     * Busca una configuración por su clave ({@code propertyName}); vacío si no
     * existe (o deshabilitada).
     */
    Optional<SystemConfiguration> findByPropertyName(String propertyName);

    /** Todas las configuraciones activas del sistema. */
    List<SystemConfiguration> findAll();
}
