package com.vetsoftware.app.appointment.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * La cita se cruza con otra del mismo veterinario (BE-17). Bloquea con 409
 * {@code APPOINTMENT_OVERLAP} salvo que el usuario reenvíe la operación con el
 * flag de forzado.
 *
 * <p>
 * El alcance del cruce es <strong>empresa + empleado</strong>, no la sede: un
 * veterinario no puede estar en dos consultas a la vez aunque sean de sedes
 * distintas.
 *
 * <p>
 * <strong>La sede sí acota lo que se cuenta.</strong> El listado de citas está
 * acotado por sede, así que devolver aquí los ids —y el nombre del veterinario—
 * de una cita de otra sede saltaba esa frontera: con el id,
 * {@code GET /appointments/{id}} (acotado solo por empresa) entrega cliente,
 * teléfono, correo y animal, y barriendo horas sucesivas se reconstruye la
 * agenda ajena. Por eso el caso de uso pasa aquí <em>solo los ids visibles para
 * el caller</em> y esta clase deriva el mensaje de esa lista:
 *
 * <ul>
 * <li>con ids visibles → mensaje completo: quién, cuándo y con qué citas;
 * <li>sin ids visibles → el horario está ocupado y nada más. El
 * {@code employeeName} recibido se <strong>ignora</strong> en esa rama, para
 * que no haya forma de filtrarlo por descuido desde un caso de uso futuro.
 * </ul>
 *
 * <p>
 * El mensaje va escrito para el usuario final —en castellano, con la hora y las
 * citas concretas— porque el front lo pinta tal cual en el aviso: es el
 * {@code detail} del ProblemDetail, no una traza para el log. Lo que va al log
 * son {@link #getEmployeeId()} y {@link #getOverlapCount()}, que diagnostican
 * igual sin arrastrar el nombre del veterinario a un canal que no lo necesita.
 */
public class AppointmentOverlapException extends RuntimeException {

    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String FORCE_HINT = " Si tienes permiso para forzar el agendamiento,"
            + " vuelve a enviarla marcando \"agendar de todos modos\".";

    private final transient List<Long> overlappingAppointmentIds;
    private final Long employeeId;
    private final int overlapCount;

    /**
     * @param employeeId
     *            veterinario cuya agenda se cruza. Solo para el log: no aparece en
     *            el mensaje.
     * @param employeeName
     *            nombre del veterinario; se usa <strong>solo</strong> si hay ids
     *            visibles.
     * @param visibleAppointmentIds
     *            citas en conflicto que el caller puede ver (mismas sedes que tiene
     *            asignadas). Vacío = el conflicto existe pero queda fuera de su
     *            alcance.
     * @param overlapCount
     *            número total de cruces, visibles o no. Solo para el log.
     */
    public AppointmentOverlapException(Long employeeId, String employeeName, LocalDateTime startAt,
            LocalDateTime endAt, List<Long> visibleAppointmentIds, int overlapCount) {
        super(buildMessage(employeeName, startAt, endAt, visibleAppointmentIds));
        this.employeeId = employeeId;
        this.overlapCount = overlapCount;
        this.overlappingAppointmentIds = visibleAppointmentIds == null
                ? List.of()
                : List.copyOf(visibleAppointmentIds);
    }

    /** Citas en conflicto <strong>visibles para el caller</strong>. Nunca null. */
    public List<Long> getOverlappingAppointmentIds() {
        return overlappingAppointmentIds;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    /** Cruces totales, incluidos los que no se le revelan al caller. */
    public int getOverlapCount() {
        return overlapCount;
    }

    private static String buildMessage(String employeeName, LocalDateTime startAt,
            LocalDateTime endAt, List<Long> visibleIds) {
        String when = when(startAt, endAt);
        if (visibleIds == null || visibleIds.isEmpty()) {
            // Honesto y sin identificar la cita: el hueco está ocupado, y no se dice
            // por quién ni con qué. Decir "de otra sede" explica por qué no aparece
            // en su agenda sin revelar cuál es esa sede.
            return "El veterinario seleccionado ya está ocupado " + when
                    + " con una cita de otra sede." + FORCE_HINT;
        }
        String who = (employeeName == null || employeeName.isBlank())
                ? "El veterinario seleccionado"
                : employeeName;
        return who + " ya tiene otra cita que se cruza con " + when + which(visibleIds) + "."
                + FORCE_HINT;
    }

    private static String when(LocalDateTime startAt, LocalDateTime endAt) {
        return startAt == null
                ? "el horario solicitado"
                : "el " + DAY.format(startAt) + " de " + HOUR.format(startAt) + " a "
                        + (endAt == null ? "?" : HOUR.format(endAt));
    }

    private static String which(List<Long> ids) {
        return ids.size() == 1
                ? " (cita " + ids.get(0) + ")"
                : " (citas " + ids.stream().map(String::valueOf).reduce((a, b) -> a + ", " + b)
                        .orElse("") + ")";
    }
}
