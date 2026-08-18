package com.vetsoftware.app.systemconfiguration.testsupport;

import com.vetsoftware.app.systemconfiguration.application.command.SetSystemConfigurationCommand;
import com.vetsoftware.app.systemconfiguration.domain.SystemConfiguration;
import java.time.LocalDateTime;

/**
 * Fixtures del modulo systemconfiguration.
 *
 * <p>
 * Se construye con el constructor publico y no con
 * {@code SystemConfiguration.create(...)}: el factory pone
 * {@code LocalDateTime.now()} y haria no deterministas las aserciones sobre
 * {@code createdDate}.
 */
public final class SystemConfigurationMother {

    public static final Long CONFIG_ID = 700L;
    public static final String PROPERTY_NAME = "uvt";
    public static final String VALUE = "47065";
    public static final LocalDateTime CREADO = LocalDateTime.of(2026, 1, 15, 10, 30);

    private SystemConfigurationMother() {
    }

    /** Configuracion habilitada, con id, tal como vendria de la base. */
    public static SystemConfiguration configuracionExistente() {
        return configuracionExistente(CONFIG_ID, PROPERTY_NAME, VALUE);
    }

    public static SystemConfiguration configuracionExistente(Long id, String propertyName,
            String value) {
        return new SystemConfiguration(id, propertyName, value, CREADO, true);
    }

    public static SetSystemConfigurationCommand comandoValido() {
        return new SetSystemConfigurationCommand(PROPERTY_NAME, VALUE);
    }
}
