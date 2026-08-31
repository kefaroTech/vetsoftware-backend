package com.vetsoftware.app.aiproposal.domain;

/**
 * Como se pinta la propuesta, en vocabulario cerrado y <strong>deliberadamente
 * pobre</strong>.
 *
 * <p>
 * &#9940; <strong>Las tres degradaciones colapsan en {@link #DETERMINISTIC}, y
 * eso es el control, no una simplificacion.</strong> {@code GenerationOutcome}
 * distingue hacia dentro el tope de gasto, la palanca apagada, la falta de
 * hints y el fallo del modelo -la telemetria de S9.2 los necesita separados-,
 * pero hacia fuera un anonimo con {@code curl} no puede saber cual de los
 * cuatro le toco: saberlo le diria <strong>cuando se agoto el presupuesto
 * diario de la plataforma</strong>. El otro canal por el que se filtraba lo
 * mismo era el tiempo, y lo cierra el suelo de latencia de S4.2.3.
 *
 * <p>
 * {@link #NOT_UNDERSTOOD} y {@link #OUT_OF_DOMAIN} si se distinguen: no son
 * estado del sistema sino lectura del texto del propio prospecto, tienen
 * pantallas distintas (anexo A S3.9 y S3.10) y confundirlas es ofrecerle
 * historia clinica veterinaria a una peluqueria de senoras.
 */
public enum ProposalPresentation {

    /** El modelo leyo el texto y la propuesta lleva sus lineas. */
    PROPOSAL,

    /** El modelo dijo que no entendio: reescribir sirve, y se le pide. */
    NOT_UNDERSTOOD,

    /**
     * El negocio no es del dominio. No se ofrece ni una linea, ni siquiera de punto
     * de partida: el error caro aqui no es perder el lead, es venderle software
     * veterinario a quien no tiene animales.
     */
    OUT_OF_DOMAIN,

    /**
     * Sin lectura del texto libre. El carrito es el determinista -nucleo, cierre de
     * dependencias y precio por tramos-, que es una propuesta correcta.
     *
     * <p>
     * &#9940; <strong>Lleva lineas, siempre.</strong> Ese es justo el limite con
     * {@link #NO_CATALOG}, y confundirlos fue lo que mando un diagnostico entero en
     * la direccion equivocada: ver alli.
     */
    DETERMINISTIC,

    /**
     * &#9940; <strong>No se sirvio NADA: ni el determinista ni el modelo llegaron a
     * correr.</strong> No hay lista de precios {@code PUBLISHED} vigente, asi que
     * no hay catalogo que cotizar; la respuesta es 200 con cero lineas, sin token y
     * sin nada persistido, porque no se abrio ningun turno.
     *
     * <p>
     * <strong>Existe porque {@link #DETERMINISTIC} tenia dos lecturas
     * incompatibles</strong> —«hubo degradacion del modelo, con lineas reales» y
     * «no se genero nada»— y el mismo valor no puede significar las dos: quien mira
     * la respuesta no puede distinguir un catalogo vacio de un modelo caido, y el
     * estado peor de los dos se lee como el mejor. El emisor es
     * {@code ProposalViewDto.sinCatalogo()}, y su unico camino son los dos returns
     * tempranos de {@code GenerateProposalService.generate}, antes del generador.
     *
     * <p>
     * <strong>No se persiste jamas.</strong> {@code ai_proposal_turns.presentation}
     * solo se escribe al cerrar un turno, y por este camino no hay turno que
     * cerrar; por eso la relectura de {@code ProposalReader} no puede devolverlo y
     * no hace falta ningun changeset.
     */
    NO_CATALOG
}
