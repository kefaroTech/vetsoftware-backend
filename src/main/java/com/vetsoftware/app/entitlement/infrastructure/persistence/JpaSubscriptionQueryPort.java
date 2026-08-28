package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.entitlement.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.entitlement.domain.CapacityGrantLine;
import com.vetsoftware.app.entitlement.domain.LimitDimensionRef;
import com.vetsoftware.app.entitlement.domain.LineChargeMode;
import com.vetsoftware.app.entitlement.domain.MeasureKind;
import com.vetsoftware.app.entitlement.domain.ContractSnapshot;
import com.vetsoftware.app.entitlement.domain.ContractStatus;
import com.vetsoftware.app.entitlement.domain.ModuleGrantLine;
import com.vetsoftware.app.entitlement.domain.ResetPeriod;
import com.vetsoftware.app.entitlement.domain.SubModuleRef;
import com.vetsoftware.app.entitlement.domain.SubscriptionRef;
import com.vetsoftware.app.entitlement.domain.TrialOutcomePolicy;
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

    /**
     * La fecha de firma, sola. Es lo unico que necesita la decision de D-74 cuando
     * un contador no tiene fila, y armar el {@code ContractSnapshot} entero --tres
     * consultas-- para leerla seria pagar el recalculo completo por una fecha.
     */
    @Override
    public Optional<LocalDate> findContractSignedOnByCompanyId(Long companyId) {
        return subscriptionJpaRepository.findEarliestStartDateByCompanyId(companyId);
    }

    private ContractSnapshot snapshotOf(Long companyId, ContractSubscriptionView view) {
        SubscriptionRef subscription = new SubscriptionRef(view.getId(),
                ContractStatus.valueOf(view.getStatus()), view.getTrialEndDate(),
                view.getStartDate());
        List<ModuleGrantLine> moduleLines = itemJpaRepository
                .findModuleLines(companyId, view.getId()).stream()
                .map(JpaSubscriptionQueryPort::toModuleLine).toList();
        List<CapacityGrantLine> capacityLines = itemJpaRepository
                .findCapacityLines(companyId, view.getId()).stream()
                .map(JpaSubscriptionQueryPort::toCapacityLine).toList();
        return new ContractSnapshot(subscription, moduleLines, capacityLines);
    }

    @Override
    public void lockContractByCompanyId(Long companyId) {
        // No se comprueba que devuelva filas: una empresa sin contrato no tiene nada
        // que bloquear, y de denunciarlo ya se encarga el recalculo con
        // CompanyWithoutContractException, que sabe explicar por que.
        subscriptionJpaRepository.lockContractsByCompanyId(companyId);
    }

    private static ModuleGrantLine toModuleLine(ContractModuleLineView view) {
        LineChargeMode chargeMode = modoDeCobro(view.getChargeMode());
        // Una linea que no esta en prueba no puede llevar fecha de prueba: la
        // columna la conserva por historia, pero el dominio la rechaza (R-TRIAL-07).
        LocalDate trialEndDate = chargeMode.isTrial() ? view.getTrialEndDate() : null;
        return new ModuleGrantLine(view.getSubscriptionItemId(),
                new SubModuleRef(view.getSubModuleId(), view.getSubModuleCode(),
                        view.getSubModuleName()),
                esCierto(view.getReadOnlyCapable()), view.getEffectiveFrom(), view.getEffectiveTo(),
                esCierto(view.getCore()), chargeMode, trialEndDate,
                chargeMode.isTrial() ? desenlace(view, trialEndDate) : null,
                esCierto(view.getDegradationImmune()));
    }

    /**
     * {@code charge_mode} nulo es {@code PAID}: es el defecto de la columna y el
     * unico modo seguro. Leer una linea desconocida como si fuera prueba la haria
     * caducar sola.
     */
    private static LineChargeMode modoDeCobro(String raw) {
        if (raw == null || raw.isBlank()) {
            return LineChargeMode.PAID;
        }
        try {
            return LineChargeMode.valueOf(raw);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalStateException("subscription_items.charge_mode has the unknown value '"
                    + raw + "'. The four modes are TRIAL, PAID, FREE_LIMITED and"
                    + " EXPIRED_READ_ONLY; a fifth one means the column was written by"
                    + " something that does not know the model", unknown);
        }
    }

    /**
     * El desenlace congelado de la concesion. <strong>Si falta, se falla en voz
     * alta.</strong>
     *
     * <p>
     * Una linea {@code TRIAL} sin concesion detras no puede escribir su fila
     * sucesora, y sin fila sucesora el acceso <em>desaparece</em> el dia del
     * vencimiento en vez de bajar --el bloqueante entero de la capa de prueba--.
     * Degradar en silencio a un desenlace por defecto seria elegir por el negocio
     * cual de los tres, y dos de los tres son decisiones de dinero.
     */
    private static TrialOutcomePolicy desenlace(ContractModuleLineView view,
            LocalDate trialEndDate) {
        String raw = view.getTrialOutcome();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Subscription item " + view.getSubscriptionItemId()
                    + " is TRIAL until " + trialEndDate + " but has no company_trial_grants row"
                    + " to freeze its outcome: without it there is no successor row to write,"
                    + " and access would vanish on expiry instead of stepping down (R-ENT-01)");
        }
        try {
            return TrialOutcomePolicy.valueOf(raw);
        } catch (IllegalArgumentException unknown) {
            throw new IllegalStateException("company_trial_grants.policy_trial_outcome has the"
                    + " unknown value '" + raw + "' for subscription item "
                    + view.getSubscriptionItemId() + ": the three outcomes are CONVERT_TO_PAID,"
                    + " LIMITED and READ_ONLY", unknown);
        }
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

    /**
     * Traduce una linea de capacidad del contrato a su eje del catalogo.
     *
     * <p>
     * <strong>Un eje vendido que no esta sembrado se denuncia aqui</strong>, y no
     * se omite. La consulta trae la linea con el {@code LEFT JOIN} sin resolver
     * justamente para que este caso sea visible: omitirla haria nacer a la empresa
     * sin ese contador, lo que el resto del sistema lee como techo cero, y el
     * cliente quedaria sin la funcion y sin explicacion. Es ruidoso a proposito
     * --el arreglo es una fila de semilla, no un despliegue--.
     */
    private static CapacityGrantLine toCapacityLine(ContractCapacityLineView view) {
        if (view.getLimitDimensionId() == null || view.getMeasureKind() == null
                || view.getAvailableFrom() == null) {
            throw new IllegalStateException("Subscription item " + view.getSubscriptionItemId()
                    + " sells capacity '" + view.getCapacityUnit()
                    + "' but there is no enabled row in limit_dimensions with that code:"
                    + " seed the dimension before the contract can grant a ceiling for it");
        }
        LimitDimensionRef dimension = new LimitDimensionRef(view.getLimitDimensionId(),
                view.getCapacityUnit(), MeasureKind.valueOf(view.getMeasureKind()),
                view.getAvailableFrom());
        return new CapacityGrantLine(view.getSubscriptionItemId(), dimension, view.getQuantity(),
                view.getIncludedQuantity(), toResetPeriod(view.getResetPeriod()),
                view.getEffectiveFrom(), view.getEffectiveTo());
    }

    /**
     * El texto crudo de {@code reset_period}, traducido aqui y no en la consulta.
     * Ausente es {@code null} legitimo --los ejes que no son de flujo lo tienen
     * prohibido--; que falte en uno de flujo lo denuncia {@link CapacityGrantLine},
     * que es donde vive la invariante.
     */
    private static ResetPeriod toResetPeriod(String value) {
        return value == null || value.isBlank() ? null : ResetPeriod.valueOf(value);
    }
}
