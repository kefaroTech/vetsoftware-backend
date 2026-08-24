package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.CapacityGrantLine;
import com.vetsoftware.app.entitlement.domain.CapacityUnit;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ContractStatus;
import com.vetsoftware.app.entitlement.domain.ModuleGrantLine;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.domain.SubscriptionRef;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce las tablas de {@code subscription}
 * y {@code catalogitem}. Traduce el contrato a los tipos del dominio propio,
 * para que ni el caso de uso ni el calculo sepan que existe otra feature.
 *
 * <p>
 * Tres consultas por recalculo --cabecera, lineas de modulo, lineas de
 * capacidad-- y ninguna dentro de un bucle.
 */
@Component
public class JpaSubscriptionQueryPort implements SubscriptionQueryPort {

    private final ContractSubscriptionJpaRepository subscriptionJpaRepository;
    private final ContractItemJpaRepository itemJpaRepository;

    public JpaSubscriptionQueryPort(ContractSubscriptionJpaRepository subscriptionJpaRepository,
            ContractItemJpaRepository itemJpaRepository) {
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.itemJpaRepository = itemJpaRepository;
    }

    @Override
    public Optional<ContractSnapshot> findCurrentContractByCompanyId(Long companyId, LocalDate on) {
        return subscriptionJpaRepository.findCurrentByCompanyId(companyId, on)
                .map(view -> snapshotOf(companyId, view));
    }

    @Override
    public Optional<ContractSnapshot> findLatestContractByCompanyId(Long companyId) {
        return subscriptionJpaRepository.findLatestByCompanyId(companyId)
                .map(view -> snapshotOf(companyId, view));
    }

    private ContractSnapshot snapshotOf(Long companyId, ContractSubscriptionView view) {
        SubscriptionRef subscription = new SubscriptionRef(view.getId(),
                ContractStatus.valueOf(view.getStatus()), view.getTrialEndDate());
        List<ModuleGrantLine> moduleLines = itemJpaRepository
                .findModuleLines(companyId, view.getId()).stream()
                .map(JpaSubscriptionQueryPort::toModuleLine).toList();
        List<CapacityGrantLine> capacityLines = itemJpaRepository
                .findCapacityLines(companyId, view.getId()).stream()
                .map(JpaSubscriptionQueryPort::toCapacityLine).toList();
        return new ContractSnapshot(subscription, moduleLines, capacityLines);
    }

    private static ModuleGrantLine toModuleLine(ContractModuleLineView view) {
        return new ModuleGrantLine(view.getSubscriptionItemId(),
                new SubModuleRef(view.getSubModuleId(), view.getSubModuleCode(),
                        view.getSubModuleName()),
                esCierto(view.getReadOnlyCapable()), view.getEffectiveFrom(), view.getEffectiveTo(),
                esCierto(view.getCore()));
    }

    /**
     * El {@code TINYINT} crudo que devuelve la proyeccion nativa, traducido a
     * booleano aqui y no en la consulta.
     *
     * <p>
     * Ver {@link ContractModuleLineView} para el porque completo (#472): MySQL no
     * tiene tipo booleano, Connector/J entrega {@code TINYINT} como {@code Byte} y
     * Spring Data no sabe convertirlo a {@code Boolean} en una proyeccion cerrada.
     * {@code null} es falso: una linea sin la columna poblada no abre nada.
     */
    private static boolean esCierto(Byte tinyint) {
        return tinyint != null && tinyint != 0;
    }

    private static CapacityGrantLine toCapacityLine(ContractCapacityLineView view) {
        return new CapacityGrantLine(view.getSubscriptionItemId(),
                CapacityUnit.valueOf(view.getCapacityUnit()), view.getQuantity(),
                view.getIncludedQuantity(), view.getEffectiveFrom(), view.getEffectiveTo());
    }
}
