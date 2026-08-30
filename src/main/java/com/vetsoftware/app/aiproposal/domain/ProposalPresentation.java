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
     */
    DETERMINISTIC
}
