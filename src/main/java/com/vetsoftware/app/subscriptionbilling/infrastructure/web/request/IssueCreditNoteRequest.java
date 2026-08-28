package com.vetsoftware.app.subscriptionbilling.infrastructure.web.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Los cargos que agrupa la nota crédito.
 *
 * <p>
 * Tienen que ser todos del mismo signo —negativo—; lo comprueba el dominio, que
 * es donde se pueden leer sus importes. Una nota crédito con signos mezclados
 * hace que la conciliación deje de cuadrar sin dar ninguna señal.
 *
 * <p>
 * <strong>Este es el que conserva el nombre simple en el contrato, y hay un
 * pacto detrás.</strong> {@code electronicdocument} tiene otro record con este
 * mismo nombre de clase y campos disjuntos —{@code reason} y
 * {@code partialAmount}—; springdoc funde los esquemas por nombre simple, no
 * por paquete, así que uno de los dos tenía que salir de la colisión. Lo hace
 * aquel, con {@code @Schema(name = "IssueElectronicCreditNoteRequest")}, porque
 * el nombre simple ya lo publicaba este record y la consola de plataforma lo
 * tiene atado en su {@code api.contract.ts}: mover el de aquí habría cambiado
 * el esquema bajo un consumidor vivo.
 *
 * <p>
 * <b>Si alguna vez hace falta mover el nombre publicado de este record, hay que
 * mirar aquel primero</b>: dos schemas con el mismo nombre no fallan el build,
 * publican el cuerpo del otro endpoint.
 */
public record IssueCreditNoteRequest(@NotEmpty List<Long> chargeIds) {
}
