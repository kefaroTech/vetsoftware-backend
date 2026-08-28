package com.vetsoftware.app.companylimitevent.infrastructure.orchestration;

import com.vetsoftware.app.companylimitevent.application.port.out.CompanyUsageAdjustmentPort;
import com.vetsoftware.app.entitlement.application.command.AdjustCompanyCapacityUsageCommand;
import com.vetsoftware.app.entitlement.application.dto.CompanyCapacityDto;
import com.vetsoftware.app.entitlement.application.port.in.AdjustCompanyCapacityUsageUseCase;
import com.vetsoftware.app.entitlement.application.port.in.FindCompanyAccessUseCase;
import org.springframework.stereotype.Component;

/**
 * Conecta la corrección de plataforma con el contador, que vive en otra rodaja.
 *
 * <p>
 * Está en {@code infrastructure/orchestration} y no en {@code application}
 * porque es el único sitio de esta feature autorizado a conocer la otra: el
 * {@code application} de una rodaja no importa nada de otra.
 *
 * <p>
 * <strong>Reusa la instrucción atómica que ya existe y no escribe
 * otra.</strong> El contador sube y comprueba el techo en una sola sentencia
 * del motor; montar aquí un segundo mecanismo —leer, calcular, guardar— sería
 * reintroducir exactamente la carrera que aquel existe para cerrar.
 */
@Component
public class EntitlementCompanyUsageAdjustmentAdapter implements CompanyUsageAdjustmentPort {

    private final AdjustCompanyCapacityUsageUseCase adjustUseCase;
    private final FindCompanyAccessUseCase findAccessUseCase;

    public EntitlementCompanyUsageAdjustmentAdapter(AdjustCompanyCapacityUsageUseCase adjustUseCase,
            FindCompanyAccessUseCase findAccessUseCase) {
        this.adjustUseCase = adjustUseCase;
        this.findAccessUseCase = findAccessUseCase;
    }

    /**
     * El identificador del eje viaja como texto de punta a punta: es el
     * {@code code} de {@code limit_dimensions}. Ya no se valida contra un enumerado
     * de cuatro valores --ese enumerado era lo que impedia corregir el consumo de
     * un eje nuevo sin desplegar--; quien lo valida es el caso de uso, resolviendo
     * el codigo contra el catalogo y fallando con el nombre del eje que falta.
     */
    @Override
    public int adjustUsage(Long companyId, String dimensionCode, int delta) {
        CompanyCapacityDto capacity = adjustUseCase
                .execute(new AdjustCompanyCapacityUsageCommand(companyId, dimensionCode, delta));
        return capacity.usedQuantity();
    }

    @Override
    public UsageSnapshot currentUsage(Long companyId, String dimensionCode) {
        return findAccessUseCase.findByCompanyId(companyId).capacities().stream()
                .filter(capacity -> capacity.dimensionCode().equals(dimensionCode)).findFirst()
                .map(capacity -> new UsageSnapshot(capacity.limitQuantity(),
                        capacity.usedQuantity()))
                .orElseThrow(() -> new IllegalArgumentException("Company " + companyId
                        + " has no capacity counter for dimension " + dimensionCode
                        + ": an absent row means limit zero, not unlimited"));
    }
}
