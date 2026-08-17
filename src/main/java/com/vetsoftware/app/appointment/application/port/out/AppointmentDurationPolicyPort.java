package com.vetsoftware.app.appointment.application.port.out;

/**
 * Duración por defecto de una cita cuando la propia cita no trae la suya
 * (BE-17).
 *
 * <p>
 * La cadena de resolución completa es: <em>campo de la cita</em> → <em>ajuste
 * de la empresa</em> → <strong>30 minutos</strong>. Este puerto cubre los dos
 * últimos eslabones: devuelve el ajuste de la empresa y, si no existe o no es
 * legible, los 30 minutos de respaldo. Nunca devuelve cero ni un negativo.
 */
public interface AppointmentDurationPolicyPort {

    /**
     * Minutos por defecto de la empresa. Siempre positivo: ante ausencia de ajuste,
     * valor no numérico o valor no positivo, devuelve el respaldo de 30 minutos.
     */
    int defaultDurationMinutes(Long companyId);
}
