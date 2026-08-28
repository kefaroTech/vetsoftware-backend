package com.vetsoftware.app.securityincident.application.command;

import com.vetsoftware.app.securityincident.domain.AffectedScope;

/**
 * Anota que una clinica quedo alcanzada por un incidente, y por que ambito.
 *
 * <p>
 * <strong>El {@code companyId} de este command no llega por el cuerpo de la
 * peticion: llega por la URL.</strong> Aqui la empresa no es el tenant que hace
 * la llamada —esta operacion solo la sirve {@code ROLE_SYSTEM}, que no tiene
 * empresa propia— sino <em>a que clinica alcanzo el incidente</em>, o sea un
 * dato del hecho. Pero {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} mira el tipo del
 * {@code @RequestBody} y baja por sus campos, y no distingue —ni debe— entre un
 * companyId legitimo y el que convierte el gate en una comparacion del numero
 * consigo mismo. La salida es la que la propia regla documenta: la empresa va
 * en un {@code @PathVariable}
 * ({@code POST /system/security-incidents/{id}/affected-companies/{companyId}})
 * y queda cubierta por la familia «por id», cuyo gate aqui es
 * {@code hasRole('SYSTEM')} a secas.
 *
 * @param affectedSubjectCount
 *            los titulares <b>de esa clinica</b>, no los del incidente entero
 */
public record RegisterAffectedCompanyCommand(Long securityIncidentId, Long companyId,
        AffectedScope affectedScope, int affectedSubjectCount) {
}
