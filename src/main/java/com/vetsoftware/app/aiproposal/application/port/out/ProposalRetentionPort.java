package com.vetsoftware.app.aiproposal.application.port.out;

import java.time.LocalDateTime;

/**
 * Las mutaciones de la politica de retencion, y <strong>solo</strong> ellas.
 *
 * <p>
 * <strong>Cada metodo escribe como mucho un lote y devuelve cuantas filas
 * movio.</strong> No hay ninguno que "haga el barrido entero": quien decide
 * cuantos lotes se pueden hacer en una pasada es el llamante, y esa decision es
 * configuracion. Un metodo sin tope escribiria millones de filas en una
 * transaccion, mantendria los bloqueos durante minutos y dejaria al resto del
 * producto esperando.
 *
 * <p>
 * <strong>Los tres pasos de anonimizacion son idempotentes y estan encadenados
 * por el estado de la fila, no por una lista de ids</strong>: el paso 1 marca
 * {@code anonymized_at}, el 2 borra el texto de los turnos <em>cuya propuesta
 * ya esta marcada</em> y el 3 borra el motivo de las lineas <em>cuyo turno
 * cuelga de una propuesta marcada</em>. Si el proceso muere entre el paso 1 y
 * el 2, la siguiente pasada recoge exactamente lo que quedo a medias — que es
 * la propiedad que hace que un barrido por lotes sea seguro.
 *
 * <p>
 * &#9940; <strong>El paso 3 no es opcional y es el que faltaba.</strong> El
 * prompt obliga al modelo a citar al prospecto en cada motivo, asi que borrar
 * {@code contact_email}, {@code input_text} y {@code raw_response} y conservar
 * "todas las lineas" dejaba filas marcadas como anonimizadas que seguian
 * diciendo <em>"le vendes a credito a una fundacion"</em>. Una fila que el
 * informe de cumplimiento cuenta como limpia y lleva dentro las palabras del
 * titular es peor que una fila sin anonimizar: la segunda se ve.
 */
public interface ProposalRetentionPort {

    /**
     * Paso 1: marca y vacia la cabecera. {@code contact_email} a {@code NULL}
     * arrastra a {@code contact_email_hash}, que es una columna generada.
     *
     * @return filas marcadas en este lote
     */
    int anonymizeProposals(LocalDateTime inactivasDesde, LocalDateTime ahora, int tamanoDeLote);

    /**
     * Paso 2: borra el texto libre y la respuesta cruda de los turnos de las
     * propuestas ya marcadas. No recibe corte: lo decide el estado del padre.
     *
     * @return turnos redactados en este lote
     */
    int redactTurns(int tamanoDeLote);

    /**
     * Paso 3: borra el motivo de las lineas de esas mismas propuestas y sella
     * {@code reason_redacted_at}, que es lo que permite a
     * {@code chk_ai_proposal_lines_model_reason} distinguir "borrado" de "ausente"
     * sin reescribir el {@code source} — la unica señal que dice que propuso el
     * modelo.
     *
     * @return lineas redactadas en este lote
     */
    int redactLineReasons(LocalDateTime ahora, int tamanoDeLote);

    /** Purga, paso 1 de 3: las lineas de los turnos purgables. */
    int purgeLines(LocalDateTime anterioresA, int tamanoDeLote);

    /** Purga, paso 2 de 3: los turnos de las propuestas purgables. */
    int purgeTurns(LocalDateTime anterioresA, int tamanoDeLote);

    /**
     * Purga, paso 3 de 3: la cabecera. <strong>Nunca una propuesta
     * convertida</strong>: su fila de {@code ai_proposal_conversions} va con
     * {@code ON DELETE RESTRICT} y el borrado fallaria de todas formas, pero
     * excluirla en el {@code WHERE} convierte un error de integridad en una
     * decision escrita.
     */
    int purgeProposals(LocalDateTime anterioresA, int tamanoDeLote);

    /**
     * Supresion a peticion del titular, por correo.
     *
     * <p>
     * &#9940; <strong>Borra tambien los motivos</strong>, no solo el correo. Un
     * borrado que deja la frase del titular escrita en la tabla de al lado no es un
     * borrado. Y no lleva {@code LIMIT}: una peticion de supresion es de un
     * titular, son unidades de filas, y dejarla a medias seria incumplir.
     *
     * <p>
     * <strong>Limite declarado:</strong> alcanza {@code contact_email} y los
     * motivos. Un correo escrito <em>dentro</em> del texto libre lo cubre la
     * anonimizacion por tiempo, no esto.
     */
    SuppressionResult suppressByContactEmail(String contactEmail, LocalDateTime ahora);

    /**
     * Lo que movio una supresion, desglosado por tabla: sin el desglose, "borradas
     * 0 filas" no distingue "ese correo no esta" de "el paso de motivos no corrio".
     */
    record SuppressionResult(int proposals, int turns, int lines) {

        public int total() {
            return proposals + turns + lines;
        }
    }
}
