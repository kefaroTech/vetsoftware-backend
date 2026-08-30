package com.vetsoftware.app.aiproposal.application.command;

import java.util.List;

/**
 * La edicion manual, que es <strong>soberana</strong>: lo que el cliente quita
 * no vuelve solo, y lo que anade se conserva aunque el modelo no lo proponga
 * (plan S8.3).
 *
 * <p>
 * El token va en el cuerpo por el mismo motivo que en el refinamiento.
 */
public record EditProposalLinesCommand(String publicToken, List<String> addedCodes,
        List<String> removedCodes, Long expectedVersion) {

    /**
     * &#9940; <strong>El techo de cada lista, y va aqui porque el
     * {@code @Size(max = 50)} del request acota cada elemento y no la
     * lista.</strong> Sin cota, un {@code PUT} anonimo escribe una fila de
     * {@code ai_proposal_lines} por codigo -{@code ProposalCart.build} emite una
     * {@code CartLine} tambien por cada rechazo- en un unico {@code saveLines}: una
     * escritura publica, sin sesion y gratis para quien la hace, proporcional a lo
     * que el cliente quiera mandar.
     *
     * <p>
     * <strong>Cuarenta es el mismo numero que
     * {@code ProposalOutputValidator}</strong> usa para la salida del modelo, y por
     * el mismo argumento: el catalogo real son 26 articulos y 13 vendibles a mano,
     * asi que 40 no estorba a nadie que este editando su carrito de verdad. Que
     * aquel numero existiera es justo lo que hacia facil creer que este caso estaba
     * cubierto —no lo estaba: aquel acota lo que dice el modelo, este lo que dice
     * el cliente—.
     */
    public static final int MAX_CODIGOS_POR_LISTA = 40;

    public EditProposalLinesCommand {
        if (publicToken == null || publicToken.isBlank())
            throw new IllegalArgumentException("publicToken is required");
        addedCodes = addedCodes == null ? List.of() : List.copyOf(addedCodes);
        removedCodes = removedCodes == null ? List.of() : List.copyOf(removedCodes);
        if (addedCodes.isEmpty() && removedCodes.isEmpty())
            throw new IllegalArgumentException("an edit must add or remove at least one line");
        if (addedCodes.size() > MAX_CODIGOS_POR_LISTA
                || removedCodes.size() > MAX_CODIGOS_POR_LISTA)
            throw new IllegalArgumentException(
                    "an edit cannot carry more than " + MAX_CODIGOS_POR_LISTA + " codes per list");
    }
}
