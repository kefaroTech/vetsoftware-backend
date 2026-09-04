package com.vetsoftware.app.securityincident.application.command;

import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import java.time.LocalDateTime;

/**
 * Da de alta un incidente de seguridad.
 *
 * <p>
 * <strong>Sin {@code companyId}, y no por la regla de siempre: es que no existe
 * la columna.</strong> {@code security_incidents} es una tabla de plataforma
 * —el incidente es de Lumbre, aunque alcance a varias clinicas— y el reparto
 * por clinica vive en {@code security_incident_companies}. Poner aqui una
 * empresa obligaria a elegir una de las alcanzadas, que es justamente lo que la
 * puente existe para no tener que hacer.
 *
 * <p>
 * <strong>Sin {@code deadlineAt}.</strong> El vencimiento no lo escribe quien
 * registra: lo calcula el caso de uso contra el calendario laboral. Aceptarlo
 * por el cuerpo permitiria declarar un plazo mas largo que el legal escribiendo
 * un numero, que es el unico modo de incumplir sin que nada avise.
 *
 * @param occurredAt
 *            cuando ocurrio de verdad, si se sabe. Nulo es legitimo: muchas
 *            veces solo se conoce la deteccion
 * @param escalatedAt
 *            <strong>el escalamiento interno</strong>, y el punto desde el que
 *            corren los quince dias habiles de la SIC. Nunca anterior a la
 *            deteccion
 * @param affectedSubjectCount
 *            total declarado de titulares alcanzados. Es un contador de
 *            conveniencia; el reparto real son las filas de la puente
 */
public record RegisterSecurityIncidentCommand(LocalDateTime detectedAt, LocalDateTime occurredAt,
        LocalDateTime escalatedAt, SecurityIncidentKind kind, IncidentSeverity severity,
        String summary, int affectedSubjectCount) {
}
