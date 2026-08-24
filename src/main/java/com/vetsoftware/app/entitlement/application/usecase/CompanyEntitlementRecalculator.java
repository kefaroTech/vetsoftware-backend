package com.vetsoftware.app.entitlement.application.usecase;

import com.vetsoftware.app.entitlement.application.dto.EntitlementRecalculationDto;
import com.vetsoftware.app.entitlement.application.port.out.AdminPermissionReconciliationPort;
import com.vetsoftware.app.entitlement.application.port.out.CompanyCapacityRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.CompanyWithoutContractException;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.EntitlementCalculator;
import com.vetsoftware.app.entitlement.domain.EntitlementRecalculation;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * La mecanica del recalculo, compartida por los dos casos de uso que lo
 * disparan: el gateado ({@code RecalculateCompanyEntitlementsUseCase}) y el
 * interno del alta ({@code InitializeCompanyEntitlementsUseCase}).
 *
 * <p>
 * <strong>No es un caso de uso ni un puerto</strong>, y por eso no vive en
 * {@code port/in}: es un colaborador de paquete que existe para que los dos
 * servicios no dupliquen la unica secuencia correcta --resolver el contrato,
 * calcular, borrar lo derivado, reinsertar, conciliar contadores--. Duplicarla
 * seria garantizar que un dia las dos ramas dejen de coincidir, y la que se
 * quedara atras es justamente la que corre en el alta, donde nadie mira.
 *
 * <p>
 * Los dos servicios que lo usan son los que llevan la anotacion de autorizacion
 * y la de transaccion; este no decide ninguna de las dos cosas.
 */
@Component
class CompanyEntitlementRecalculator {

    private final SubscriptionQueryPort subscriptionQueryPort;
    private final CompanyEntitlementRepository entitlementRepository;
    private final CompanyCapacityRepository capacityRepository;
    private final AdminPermissionReconciliationPort adminPermissionReconciliationPort;
    private final Clock clock;

    CompanyEntitlementRecalculator(SubscriptionQueryPort subscriptionQueryPort,
            CompanyEntitlementRepository entitlementRepository,
            CompanyCapacityRepository capacityRepository,
            AdminPermissionReconciliationPort adminPermissionReconciliationPort, Clock clock) {
        this.subscriptionQueryPort = subscriptionQueryPort;
        this.entitlementRepository = entitlementRepository;
        this.capacityRepository = capacityRepository;
        this.adminPermissionReconciliationPort = adminPermissionReconciliationPort;
        this.clock = clock;
    }

    /**
     * Reconstruye los permisos derivados y los contadores de una empresa desde su
     * contrato. Idempotente: dos ejecuciones seguidas dejan el mismo estado.
     *
     * @throws CompanyWithoutContractException
     *             si la empresa no tiene ningun contrato, ni vigente ni pasado. El
     *             recalculo <strong>no toca la tabla</strong> en ese caso: vaciar
     *             los permisos porque no encontramos el contrato dejaria a la
     *             empresa dentro del sistema sin poder hacer nada y sin ningun
     *             mensaje que lo explique.
     */
    EntitlementRecalculationDto recalculate(Long companyId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        ContractSnapshot contract = subscriptionQueryPort
                .findCurrentContractByCompanyId(companyId, today)
                .or(() -> subscriptionQueryPort.findLatestContractByCompanyId(companyId))
                .orElseThrow(() -> new CompanyWithoutContractException(
                        "Company has no subscription to derive entitlements from: " + companyId));

        // Se leen ANTES de borrar: son las unicas filas que el contrato no puede
        // reconstruir, y sus submodulos quedan fuera del calculo para no chocar
        // contra uq_company_entitlements al reinsertar.
        List<CompanyEntitlement> manualGrants = entitlementRepository
                .findManualGrantsByCompanyId(companyId);

        EntitlementRecalculation recalculated = EntitlementCalculator.recalculate(companyId,
                contract, now, manualGrants);

        // Borrado fisico de lo derivado + reinsercion, en la misma transaccion: es lo
        // que hace que recalcular dos veces deje exactamente el mismo estado.
        entitlementRepository.deleteDerivedByCompanyId(companyId);
        entitlementRepository.saveAll(recalculated.entitlements());

        List<CompanyCapacity> capacities = EntitlementCalculator.reconcile(
                capacityRepository.findAllByCompanyId(companyId), recalculated.capacities(), now);
        capacityRepository.saveAll(capacities);

        // Proyeccion operativa para el ADMIN. La autorizacion por request no confia en
        // esta copia: vuelve a cruzar permisos y entitlements vigentes siempre.
        adminPermissionReconciliationPort.reconcile(companyId, now);

        return new EntitlementRecalculationDto(companyId, contract.subscription().id(),
                contract.subscription().status().name(),
                recalculated.entitlements().size() + manualGrants.size(), manualGrants.size(),
                capacities.size(), now);
    }
}
