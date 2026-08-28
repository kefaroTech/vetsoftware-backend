package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.application.port.out.OverageAllowancePort;
import com.vetsoftware.app.entitlement.domain.OverageAllowance;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta rodaja que conoce {@code subscription_item_limits}.
 * Traduce el permiso de excedente al companion VO propio, para que ni el caso
 * de uso ni el dominio sepan que existe otra feature.
 *
 * <p>
 * <strong>Una sola consulta, y solo en la rama que ya iba a negar.</strong> El
 * consumo que cabe bajo el techo no llega hasta aqui: el camino caliente sigue
 * siendo una sentencia contra {@code company_capacities} y nada mas.
 */
@Component
public class JpaOverageAllowancePort implements OverageAllowancePort {

    private final CompanyCapacityJpaRepository jpaRepository;

    public JpaOverageAllowancePort(CompanyCapacityJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<OverageAllowance> findAllowance(Long companyId, Long limitDimensionId,
            LocalDate on) {
        return jpaRepository.findOverageAllowance(companyId, limitDimensionId, on)
                .map(view -> new OverageAllowance(view.getSubscriptionItemId(),
                        view.getSubscriptionId(), view.getUnitAmount()));
    }
}
