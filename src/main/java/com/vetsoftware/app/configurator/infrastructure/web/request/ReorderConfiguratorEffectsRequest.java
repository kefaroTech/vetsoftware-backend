package com.vetsoftware.app.configurator.infrastructure.web.request;

import com.vetsoftware.app.configurator.domain.ConfiguratorEffect;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * El reparto de prioridades tal como llega por HTTP.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>, y aquí no hay ninguno que pudiera ir:
 * el configurador es un catálogo global de plataforma y sus tablas no tienen
 * empresa.
 *
 * <p>
 * <strong>El {@code @Valid} de la lista es la mitad que se olvida.</strong> El
 * {@code @Valid} del {@code @RequestBody} dispara el validador sobre este
 * record, pero las restricciones de {@link EffectPriorityRequest} viven en el
 * argumento genérico de la lista y no se evalúan si no se marca también aquí —
 * la misma omisión que documenta la incidencia #135, con el agravante de que el
 * OpenAPI seguiría anunciando el rango al front.
 *
 * @param priorities
 *            al menos un par. Una lista vacía no es un reordenado: es una
 *            llamada que no hace nada y que quien la mandó creería que sí hizo
 *            algo
 */
public record ReorderConfiguratorEffectsRequest(
        @NotEmpty(message = "Debes indicar al menos un efecto que reordenar.") @Valid List<EffectPriorityRequest> priorities) {

    /**
     * @param priority
     *            el rango es el de {@code chk_configurator_effects_priority}, leído
     *            de las constantes del dominio para que las dos mitades no puedan
     *            divergir: si mañana la comprobación de la base se ensancha, aquí
     *            se ensancha sola
     */
    public record EffectPriorityRequest(
            @NotNull(message = "Debes indicar el efecto que se reordena.") Long effectId,
            @NotNull(message = "Debes indicar la prioridad del efecto.") @Min(value = ConfiguratorEffect.MIN_PRIORITY, message = "La prioridad no puede ser negativa.") @Max(value = ConfiguratorEffect.MAX_PRIORITY, message = "La prioridad no puede superar 9999.") Integer priority) {
    }
}
