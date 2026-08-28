package com.vetsoftware.app.securityincident.infrastructure.web.request;

import com.vetsoftware.app.securityincident.domain.AffectedScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * <strong>Sin {@code companyId}: la clinica viaja en la URL.</strong>
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO} —regla dura— examina el tipo del
 * {@code @RequestBody} y baja por sus campos, y no distingue —ni debe— entre un
 * {@code companyId} legitimo y el que convierte
 * {@code @authz.isMyCompany(#command.companyId)} en una comparacion del numero
 * consigo mismo. La salida que la propia regla documenta es esta: llevar la
 * empresa a un {@code @PathVariable} y dejar que la cubra la familia «por id»,
 * cuyo gate aqui es {@code hasRole('SYSTEM')} a secas.
 *
 * <p>
 * El endpoint queda
 * {@code POST /system/security-incidents/{id}/affected-companies/{companyId}}.
 *
 * <p>
 * <strong>Este es el que conserva el nombre simple en el contrato, y hay un
 * pacto detras.</strong> {@code externalinvoicingoutage} tiene otro record con
 * este mismo nombre de clase y campos disjuntos; springdoc funde los esquemas
 * por nombre simple, asi que uno de los dos tenia que salir de la colision. Lo
 * hace aquel, con
 * {@code @Schema(name = "RegisterOutageAffectedCompanyRequest")}, por simetria
 * con las respuestas —{@code AffectedCompanyResponse} aqui,
 * {@code OutageAffectedCompanyResponse} alli—. <b>Si alguna vez hace falta
 * mover el nombre publicado de este record, hay que mirar aquel primero</b>:
 * dos schemas con el mismo nombre no fallan el build, publican el cuerpo del
 * otro endpoint.
 *
 * @param affectedScope
 *            entra en {@code uq_sic_pair}: la misma clinica puede constar dos
 *            veces en el mismo incidente si quedo alcanzada por dos cosas
 *            distintas
 * @param affectedSubjectCount
 *            los titulares <b>de esa clinica</b>, no los del incidente entero
 */
public record RegisterAffectedCompanyRequest(
        @NotNull(message = "Debes indicar el ambito alcanzado.") @Schema(description = "Entra en la unicidad: la misma clinica puede constar con dos ambitos distintos.") AffectedScope affectedScope,
        @PositiveOrZero(message = "El numero de titulares afectados no puede ser negativo.") int affectedSubjectCount) {
}
