package com.vetsoftware.app.aiproposal.domain;

/**
 * Quien puso la linea en la propuesta ({@code chk_ai_proposal_lines_source}).
 *
 * <p>
 * <strong>{@code MODEL_RECOMMENDED} es un valor propio y no una columna
 * {@code necessity} aparte</strong> (plan S4.4): la pregunta que responde esta
 * columna es "quien puso esta linea aqui", y "el modelo, como opcional"
 * responde a esa misma pregunta. Una columna aparte obligaria a un
 * {@code CHECK} cruzado entre las dos para impedir el estado imposible
 * "{@code CUSTOMER} + recomendado".
 *
 * <p>
 * <strong>Un recomendado NO entra al carrito por defecto y NO dispara el cierre
 * de {@code REQUIRES}</strong>: cerrar dependencias de algo que nadie pidio es
 * como un carrito de 6 lineas se convierte en uno de 10. Cuando el cliente lo
 * acepta, la linea se reescribe con {@code CUSTOMER} y ahi si se cierran sus
 * requisitos.
 */
public enum LineSource {

    MODEL,

    MODEL_RECOMMENDED,

    DEPENDENCY_CLOSURE,

    CUSTOMER;

    /** Las dos fuentes que exigen motivo escrito ({@code chk_..._model_reason}). */
    public boolean exigeMotivo() {
        return this == MODEL || this == MODEL_RECOMMENDED;
    }

    /** Lo que el modelo ofrece sin meterlo en el total. */
    public boolean esRecomendacion() {
        return this == MODEL_RECOMMENDED;
    }
}
