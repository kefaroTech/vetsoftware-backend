package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.dto.CompanyAccessDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.dto.CompanyEntitlementDto;
import com.vetsoftware.app.entitlement.application.port.in.FindCompanyAccessUseCase;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La lectura que hace cada peticion. Dos consultas acotadas por empresa y el
 * filtro de vigencia en memoria: por empresa hay del orden de 15-40 permisos y
 * un indice mas por {@code access_level}/{@code valid_until} pagaria escritura
 * en cada recalculo para no ahorrar nada.
 *
 * <p>
 * La caducidad se evalua aqui contra el reloj inyectado, no con un proceso
 * programado: <strong>una prueba caduca sola a la fecha</strong> y no hay
 * ningun trabajo que se pueda olvidar de correr.
 */
@Observed(name = "entitlement.access")
@Service
public class FindCompanyAccessService implements FindCompanyAccessUseCase {

    private final CompanyEntitlementRepository entitlementRepository;
    private final CompanyCapacityRepository capacityRepository;
    private final Clock clock;

    public FindCompanyAccessService(CompanyEntitlementRepository entitlementRepository,
            CompanyCapacityRepository capacityRepository, Clock clock) {
        this.entitlementRepository = entitlementRepository;
        this.capacityRepository = capacityRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyAccessDto findByCompanyId(Long companyId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<CompanyEntitlement> all = entitlementRepository.findAllByCompanyId(companyId);
        List<CompanyEntitlementDto> granted = all.stream()
                .filter(entitlement -> entitlement.grantsAt(now)).map(CompanyEntitlementDto::from)
                .toList();
        List<CompanyCapacityDto> capacities = capacityRepository.findAllByCompanyId(companyId)
                .stream().map(CompanyCapacityDto::from).toList();
        return new CompanyAccessDto(companyId, granted, capacities, oldestRecalculation(all));
    }

    /**
     * El mas antiguo de la empresa, que es el que delata un proceso caido. Devolver
     * el mas reciente escondería justo el caso que hay que ver.
     */
    private static LocalDateTime oldestRecalculation(List<CompanyEntitlement> entitlements) {
        return entitlements.stream().map(CompanyEntitlement::getRecalculatedAt)
                .min(Comparator.naturalOrder()).orElse(null);
    }
}
