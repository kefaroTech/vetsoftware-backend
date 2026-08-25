package com.vetsoftware.app.platformaccess.infrastructure.persistence;

import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessSwitchPort;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaEntity;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaRepository;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Lee el interruptor del formulario de {@code system_configurations}, el
 * almacen clave-valor global que el repositorio ya tiene. Es el unico cruce
 * permitido de vertical slicing: persistencia contra persistencia de otra
 * feature.
 *
 * <p>
 * <b>Fallo seguro.</b> Fila ausente, valor vacio o texto que no sea
 * {@code true}: cerrado. No lleva {@code CHECK} en la base porque la columna es
 * generica y compartida con el UVT y con todo lo demas; la interpretacion del
 * texto es de la aplicacion, y ante un valor ilegible la unica lectura sensata
 * es la que no abre un formulario que acuna superadministradores.
 */
@Component
public class JpaPlatformAccessSwitchPort implements PlatformAccessSwitchPort {

    static final String PROPERTY_NAME = "platform.access-request.open";

    private final SystemConfigurationJpaRepository systemConfigurationJpaRepository;

    public JpaPlatformAccessSwitchPort(
            SystemConfigurationJpaRepository systemConfigurationJpaRepository) {
        this.systemConfigurationJpaRepository = systemConfigurationJpaRepository;
    }

    @Override
    public boolean isOpen() {
        return systemConfigurationJpaRepository.findByPropertyName(PROPERTY_NAME)
                .map(SystemConfigurationJpaEntity::getValue)
                .map(value -> "true".equals(value.trim().toLowerCase(Locale.ROOT))).orElse(false);
    }
}
