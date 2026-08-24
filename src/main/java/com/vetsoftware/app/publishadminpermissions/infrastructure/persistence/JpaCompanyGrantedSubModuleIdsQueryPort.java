package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanyEntitlementJpaRepository;
import com.vetsoftware.app.entitlement.infrastructure.persistence.CompanySubModuleGrantView;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyGrantedSubModuleIdsQueryPort;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JpaCompanyGrantedSubModuleIdsQueryPort implements CompanyGrantedSubModuleIdsQueryPort {

    /**
     * {@code NONE} queda fuera: es la baja de un modulo, y republicar permisos
     * sobre el volveria a abrir la pantalla que la baja acaba de cerrar.
     */
    private static final List<String> NIVELES_CONCEDIDOS = List.of("FULL", "READ_ONLY");

    private final CompanyEntitlementJpaRepository companyEntitlementJpaRepository;

    public JpaCompanyGrantedSubModuleIdsQueryPort(
            CompanyEntitlementJpaRepository companyEntitlementJpaRepository) {
        this.companyEntitlementJpaRepository = companyEntitlementJpaRepository;
    }

    @Override
    public Map<Long, Set<Long>> findGrantedSubModuleIdsByCompanyIds(Set<Long> companyIds) {
        if (companyIds.isEmpty())
            return Map.of();
        Map<Long, Set<Long>> result = new HashMap<>();
        for (CompanySubModuleGrantView grant : companyEntitlementJpaRepository
                .findGrantedSubModuleIdsByCompanyIdIn(companyIds, NIVELES_CONCEDIDOS)) {
            result.computeIfAbsent(grant.getCompanyId(), k -> new HashSet<>())
                    .add(grant.getSubModuleId());
        }
        return result;
    }
}
