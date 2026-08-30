package com.vetsoftware.app.aiproposal.infrastructure.web.request;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * La edicion manual del carrito.
 *
 * <p>
 * <strong>{@code version} es el bloqueo optimista, y no es opcional por
 * comodidad.</strong> Dos pestanas son dos clientes sobre la misma propuesta
 * sin sesion que los serialice; sin la version, un refinamiento en vuelo pisa
 * esta edicion y devuelve la linea que el usuario acababa de quitar.
 *
 * <p>
 * &#9940; <strong>El {@code @Size} de dentro del {@code List<>} acota CADA
 * ELEMENTO, no la lista.</strong> Es la trampa que dejo este {@code PUT}
 * anonimo sin cota ninguna: {@code max = 50} son los 50 caracteres de
 * {@code item_code VARCHAR(50)} de un codigo, y con eso escrito el cuerpo podia
 * traer cien mil codigos de un caracter. {@code ProposalCart.build} emite una
 * {@code CartLine} por codigo —tambien por cada rechazo— y
 * {@code ProposalTurnWriter.escribirEdicion} las persiste todas en un solo
 * {@code saveLines}: una escritura publica, sin sesion y sin coste para quien
 * la hace, proporcional a lo que el cliente quiera escribir. Y el
 * {@code MAX_CODES = 40} de {@code ProposalOutputValidator} <strong>no</strong>
 * cubria esto: aquel acota la salida del modelo, no la del cliente.
 *
 * <p>
 * El techo de la lista es el mismo 40 de {@code ProposalOutputValidator}, y por
 * el mismo motivo: el catalogo real son 26 articulos —13 vendibles a mano—, asi
 * que 40 no estorba a nadie que este editando de verdad. La cota se repite en
 * {@code EditProposalLinesCommand}, que es donde la comprueba tambien el camino
 * que no pasa por el binder.
 */
public record EditProposalLinesRequest(@NotBlank @Size(min = 43, max = 43) String token,
        @Size(max = EditProposalLinesCommand.MAX_CODIGOS_POR_LISTA) List<@NotBlank @Size(max = 50) String> addedCodes,
        @Size(max = EditProposalLinesCommand.MAX_CODIGOS_POR_LISTA) List<@NotBlank @Size(max = 50) String> removedCodes,
        Long version) {
}
