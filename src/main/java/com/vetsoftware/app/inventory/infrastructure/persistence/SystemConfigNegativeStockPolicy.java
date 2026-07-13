package com.vetsoftware.app.inventory.infrastructure.persistence;

import com.vetsoftware.app.inventory.application.port.out.NegativeStockPolicyPort;
import com.vetsoftware.app.systemconfiguration.infrastructure.persistence.SystemConfigurationJpaRepository;
import org.springframework.stereotype.Component;

/**
 * Política de stock negativo respaldada por {@code system_configurations}.
 *
 * <p>Nota: {@code system_configurations} es un almacén GLOBAL (clave única {@code property_name},
 * sin {@code company_id}), así que hoy el flag {@code inventory.allow_negative_stock} aplica a
 * todas las empresas. La granularidad por empresa queda como seguimiento futuro; la firma del
 * puerto ya recibe {@code companyId} para no tener que cambiarla cuando se migre.
 */
@Component
public class SystemConfigNegativeStockPolicy implements NegativeStockPolicyPort {

    private static final String KEY = "inventory.allow_negative_stock";

    private final SystemConfigurationJpaRepository systemConfigurationJpaRepository;

    public SystemConfigNegativeStockPolicy(SystemConfigurationJpaRepository systemConfigurationJpaRepository) {
        this.systemConfigurationJpaRepository = systemConfigurationJpaRepository;
    }

    @Override
    public boolean allowsNegative(Long companyId) {
        return systemConfigurationJpaRepository.findByPropertyName(KEY)
            .map(e -> "true".equalsIgnoreCase(e.getValue()))
            .orElse(false);
    }
}
