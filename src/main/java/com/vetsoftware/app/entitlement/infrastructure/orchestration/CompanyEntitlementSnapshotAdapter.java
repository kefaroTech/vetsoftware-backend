package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import com.vetsoftware.app.companyentitlementsnapshot.application.command.RecordEntitlementSnapshotCommand;
import com.vetsoftware.app.companyentitlementsnapshot.application.port.in.RecordEntitlementSnapshotUseCase;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotActor;
import com.vetsoftware.app.companyentitlementsnapshot.domain.SnapshotTriggerReason;
import com.vetsoftware.app.entitlement.application.port.out.EntitlementSnapshotPort;
import com.vetsoftware.app.entitlement.domain.CompanyCapacity;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.SnapshotReason;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * El unico archivo de este slice que conoce la feature de las fotos. Traduce el
 * resultado del recalculo a su command, para que ni el calculo ni el caso de
 * uso sepan que existe otra feature.
 *
 * <p>
 * <strong>El numero de version del formato va DENTRO del payload</strong>
 * (R-ENT-15). Sin el, renombrar una clave del documento hace que las consultas
 * sobre fotos viejas devuelvan vacio en silencio: no fallan, no avisan,
 * devuelven cero filas y parecen una empresa que no tenia permisos.
 */
@Component
public class CompanyEntitlementSnapshotAdapter implements EntitlementSnapshotPort {

    /**
     * Version 1 del documento. <strong>Se sube cada vez que cambia una
     * clave</strong>, y las consultas sobre fotos viejas tienen que ramificar por
     * ella.
     */
    private static final int FORMATO = 1;

    private final RecordEntitlementSnapshotUseCase recordSnapshot;
    private final ObjectMapper objectMapper;

    public CompanyEntitlementSnapshotAdapter(RecordEntitlementSnapshotUseCase recordSnapshot,
            ObjectMapper objectMapper) {
        this.recordSnapshot = recordSnapshot;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(Long companyId, LocalDateTime recalculatedAt, SnapshotReason reason,
            List<CompanyEntitlement> entitlements, List<CompanyCapacity> capacities) {
        // El recalculo lo dispara un proceso, tambien cuando lo pide una persona:
        // quien firma el hecho es el sistema que lo ejecuto.
        recordSnapshot.execute(new RecordEntitlementSnapshotCommand(companyId,
                SnapshotActor.automatedProcess(), triggerReason(reason), null,
                payload(recalculatedAt, entitlements, capacities), FORMATO));
    }

    private String payload(LocalDateTime recalculatedAt, List<CompanyEntitlement> entitlements,
            List<CompanyCapacity> capacities) {
        Map<String, Object> documento = new LinkedHashMap<>();
        documento.put("formatVersion", FORMATO);
        documento.put("recalculatedAt", recalculatedAt.toString());
        documento.put("entitlements", permisos(entitlements));
        documento.put("capacities", contadores(capacities));
        try {
            return objectMapper.writeValueAsString(documento);
        } catch (JacksonException imposible) {
            // Solo mapas, listas, cadenas y numeros: si esto falla, lo que hay roto
            // es el ObjectMapper. Callarlo dejaria fotos vacias sin que nadie lo
            // notara hasta que alguien reclamara por un acceso de hace medio ano.
            throw new IllegalStateException("Could not serialise the entitlement snapshot payload",
                    imposible);
        }
    }

    private static List<Map<String, Object>> permisos(List<CompanyEntitlement> entitlements) {
        List<Map<String, Object>> filas = new ArrayList<>(entitlements.size());
        for (CompanyEntitlement permiso : entitlements) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("subModuleId", permiso.getSubModule().id());
            fila.put("subModuleCode", permiso.getSubModule().code());
            fila.put("accessLevel", permiso.getAccessLevel().name());
            fila.put("source", permiso.getSource().name());
            fila.put("subscriptionItemId", permiso.getSubscriptionItemId());
            fila.put("validFrom", permiso.getValidFrom().toString());
            fila.put("validUntil",
                    permiso.getValidUntil() == null ? null : permiso.getValidUntil().toString());
            filas.add(fila);
        }
        return filas;
    }

    private static List<Map<String, Object>> contadores(List<CompanyCapacity> capacities) {
        List<Map<String, Object>> filas = new ArrayList<>(capacities.size());
        for (CompanyCapacity contador : capacities) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("dimensionId", contador.getDimension().id());
            fila.put("dimensionCode", contador.getDimension().code());
            fila.put("periodKey", contador.getPeriodKey().value());
            fila.put("limitQuantity", contador.getLimitQuantity());
            fila.put("usedQuantity", contador.getUsedQuantity());
            filas.add(fila);
        }
        return filas;
    }

    private static SnapshotTriggerReason triggerReason(SnapshotReason reason) {
        return switch (reason) {
            case CONTRACT_AMENDMENT -> SnapshotTriggerReason.CONTRACT_AMENDMENT;
            case TRIAL_EXPIRED -> SnapshotTriggerReason.TRIAL_EXPIRED;
            case DUNNING -> SnapshotTriggerReason.DUNNING;
            case MANUAL -> SnapshotTriggerReason.MANUAL;
            case REPAIR -> SnapshotTriggerReason.REPAIR;
        };
    }
}
