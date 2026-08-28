package com.vetsoftware.app.bankreceipt.application.command;

/**
 * Sacar una entrada de la bandeja porque ya se sabe de quien era.
 *
 * <p>
 * <strong>Solo lleva el id, y eso es lo que el esquema permite decir
 * hoy.</strong> La tabla no tiene columna para el cliente identificado: quien
 * apunta a quien es la liquidacion de la pasarela, que referencia al extracto y
 * no al reves. Identificar aqui es exclusivamente marcar «esta ya no es trabajo
 * pendiente» y dejar constancia de cuando se dejo de buscar.
 *
 * <p>
 * Es un {@code record} de un solo componente a proposito, y no un {@code Long}
 * suelto en la firma del puerto: el dia que el esquema añada el dueño, el campo
 * entra aqui sin cambiar la firma ni el {@code @PreAuthorize} que la
 * referencia.
 */
public record IdentifyBankReceiptCommand(Long id) {
}
