package com.vetsoftware.app.configurator.application.command;

import java.util.List;

/**
 * Reparto completo de prioridades sobre un conjunto de efectos.
 *
 * <p>
 * <strong>Es una operación de conjunto y no una de fila, y eso es la
 * decisión.</strong> Mover un efecto de sitio casi nunca es mover uno: si el
 * {@code REMOVE} de «soy solo estética» tiene que caer después del {@code ADD}
 * de «vendo productos», lo que se está corrigiendo es el orden
 * <em>relativo</em> de los dos. Un endpoint por efecto obligaría a la pantalla
 * a mandar N peticiones y dejaría el conjunto en un estado intermedio
 * incoherente entre una y otra — y ese estado intermedio es exactamente el que
 * produce un carrito equivocado.
 *
 * <p>
 * <strong>No lleva {@code companyId} y no hay ninguno que pudiera
 * llevar.</strong> El configurador es un catálogo global de plataforma: las
 * tres tablas que lo componen no tienen empresa, porque el cuestionario que ve
 * un prospecto es el mismo para todos.
 *
 * @param priorities
 *            los pares (efecto, prioridad) a aplicar. No tiene que cubrir todos
 *            los efectos: lo que no se nombra se queda como está
 */
public record ReorderConfiguratorEffectsCommand(List<EffectPriority> priorities) {

    /**
     * Un efecto y el sitio que se le asigna en el orden de aplicación.
     *
     * @param priority
     *            {@code int} y no {@code Integer}: aquí ya no hay hueco para el
     *            nulo. La ausencia de valor la caza el {@code @NotNull} del request
     *            en el binder, que es donde produce un 400 con el campo nombrado;
     *            dejarla llegar hasta aquí solo serviría para elegir en silencio un
     *            valor por defecto que nadie pidió
     */
    public record EffectPriority(Long effectId, int priority) {
    }
}
