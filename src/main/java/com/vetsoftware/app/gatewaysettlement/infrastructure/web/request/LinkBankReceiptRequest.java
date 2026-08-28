package com.vetsoftware.app.gatewaysettlement.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * La linea del extracto por la que entro el neto del lote.
 *
 * <p>
 * <strong>Va en el cuerpo y no en la ruta</strong> porque no es un
 * identificador del recurso que se esta modificando —ese es el {@code {id}} del
 * lote— sino el dato de la operacion. Una ruta con los dos ids leeria como si
 * el recurso fuera el par, y el par no existe: la relacion es una columna del
 * lote.
 */
public record LinkBankReceiptRequest(
        @NotNull(message = "Debes indicar la entrada del extracto bancario.") @Positive(message = "El identificador de la entrada del extracto no es valido.") Long bankReceiptId) {
}
