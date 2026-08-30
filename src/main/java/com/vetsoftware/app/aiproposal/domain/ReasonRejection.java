package com.vetsoftware.app.aiproposal.domain;

/**
 * Las nueve reglas del saneador del motivo (plan S6.4.1), como
 * <strong>vocabulario cerrado</strong>.
 *
 * <p>
 * Es la etiqueta {@code rule} de la metrica
 * {@code vetsoftware.ai.proposal.reason_rejected}, y por eso es un enum y no
 * una cadena: una etiqueta de cardinalidad abierta sobre texto que escribe un
 * modelo es una bomba de series temporales. Si la regla 3 dispara en el 40 % de
 * las lineas, el problema es el prompt y no el saneador — y sin contador eso no
 * se ve nunca.
 *
 * <p>
 * <strong>{@link #R2_LARGO} es la unica que no sustituye.</strong> Un motivo
 * demasiado largo se trunca y sigue siendo del modelo; las otras ocho caen al
 * {@code short_description} determinista.
 */
public enum ReasonRejection {

    /**
     * Vacio o con menos de 10 caracteres utiles: tres palabras no explican nada.
     */
    R1_CORTO(true),

    /** Mas de 140 caracteres. Se trunca en el ultimo espacio; no se descarta. */
    R2_LARGO(false),

    /**
     * Contiene cualquier digito. Deliberadamente burda: el prompt <em>obliga</em>
     * al modelo a citar al cliente, y el cliente escribe "facturamos 40 millones al
     * mes". Cazar solo rachas de cuatro o mas digitos deja pasar {@code 900},
     * {@code 12} y {@code 19 %} — y una cifra alucinada pintada al lado de un
     * precio real es indistinguible del precio calculado.
     */
    R3_CIFRA(true),

    /** Dinero escrito con letras: el digito solo no basta para "dos millones". */
    R4_DINERO(true),

    /** Etiquetas y esquemas de URI. Cinturon sobre la interpolacion de Vue. */
    R5_MARCADO(true),

    /** Enlaces y dominios: es el vector de phishing de S6.4. */
    R6_ENLACE(true),

    /** Un codigo del catalogo en mayusculas es el oraculo de S6.5 en prosa. */
    R7_CODIGO(true),

    /** Correo o telefono del propio prospecto, citado por el modelo. */
    R8_CONTACTO(true),

    /** La misma frase en mas de tres lineas del turno: el modelo se atasco. */
    R9_REPETIDO(true);

    private final boolean sustituye;

    ReasonRejection(boolean sustituye) {
        this.sustituye = sustituye;
    }

    /** {@code true} si el motivo cae al {@code short_description} determinista. */
    public boolean sustituye() {
        return sustituye;
    }
}
