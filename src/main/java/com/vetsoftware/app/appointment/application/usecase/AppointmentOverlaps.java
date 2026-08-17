package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository.Overlap;
import java.util.List;
import java.util.Set;

/**
 * Decide qué parte de un cruce de agenda se le puede contar al caller (BE-17).
 *
 * <p>
 * El cruce se calcula por empresa + empleado —sin sede— porque un veterinario
 * no puede estar en dos sitios a la vez. La <em>visibilidad</em>, en cambio, sí
 * es por sede: el listado de citas pasa por
 * {@code Authz.resolveAccessibleBranch(...)}, así que revelar el id de una cita
 * de otra sede cruza esa frontera y, con {@code GET /appointments/{id}} acotado
 * solo por empresa, entrega la cita entera.
 *
 * <p>
 * <strong>Falla cerrado.</strong> Un alcance nulo o vacío no significa «todas
 * las sedes», significa «no reveles ninguna»: así un caller futuro que olvide
 * rellenar el alcance del command pierde detalle en el mensaje en vez de
 * filtrar la agenda entera.
 */
final class AppointmentOverlaps {

    private AppointmentOverlaps() {
    }

    /**
     * Ids de los cruces que el caller sí puede consultar: los de las sedes que
     * tiene asignadas. Una cita sin sede (dato antiguo) se considera fuera de
     * alcance.
     */
    static List<Long> visibleIds(List<Overlap> overlaps, Set<Long> visibleBranchIds) {
        if (overlaps == null || overlaps.isEmpty() || visibleBranchIds == null
                || visibleBranchIds.isEmpty()) {
            return List.of();
        }
        return overlaps.stream()
                .filter(o -> o.branchId() != null && visibleBranchIds.contains(o.branchId()))
                .map(Overlap::id).toList();
    }

    /**
     * Todos los ids en conflicto — para el log del forzado, nunca para la
     * respuesta.
     */
    static List<Long> allIds(List<Overlap> overlaps) {
        return overlaps.stream().map(Overlap::id).toList();
    }
}
