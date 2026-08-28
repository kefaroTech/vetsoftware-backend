package com.vetsoftware.app.companyusageevent.domain;

/**
 * El eje contable al que pertenece un hecho de uso, que es al mismo tiempo
 * <b>el nombre de la columna donde vive su referencia</b>.
 *
 * <p>
 * Espejo exacto de {@code chk_cue_branch} (changeset 354), que hace dos cosas a
 * la vez y las dos importan. La primera: impide que un hecho del eje de
 * mascotas apunte a una cita. La segunda, menos evidente y mas valiosa:
 * <strong>impide que exista un hecho de uso para un eje de existencias</strong>
 * —{@code USER}, {@code BRANCH}, {@code TERMINAL}, {@code STORAGE_GB}—. Esos no
 * se acumulan hecho a hecho, <em>se cuentan</em>: preguntar cuantas sucursales
 * hay es un {@code COUNT} sobre la tabla de sucursales, no una suma de eventos.
 * Anotarlos aqui produciria un contador que se dispara solo y un excedente
 * cobrado sobre algo que nadie consumio.
 *
 * <p>
 * <strong>Por que es un enum de cuatro valores y no un {@code String}
 * libre.</strong> El dominio guarda el eje y <b>una sola</b> referencia
 * ({@code usageReferenceId}); el reparto a una de las cuatro columnas nulables
 * lo hace el mapper. Asi la combinacion prohibida —dos ramas rellenas, o
 * ninguna— <em>no se puede representar</em> en Java, en vez de ser
 * representable y estar vigilada por un validador que alguien puede olvidar
 * llamar. Es la misma diferencia que hay entre una regla y un recuerdo.
 *
 * <p>
 * Los cuatro nombres son literalmente los codigos de {@code limit_dimensions}
 * que la clave foranea compuesta {@code fk_cue_dimension (limit_dimension_id,
 * limit_dimension_code)} exige que coincidan con la fila del catalogo. Cambiar
 * uno aqui no renombra nada: rompe la clave foranea.
 *
 * <p>
 * <strong>Consecuencia honesta, escrita para quien venda un eje nuevo:</strong>
 * vender un eje <b>contable</b> nuevo es un despliegue —columna nueva, clave
 * nueva, {@code CHECK} reescrito y un valor mas aqui—. Vender un eje <b>de
 * existencias</b> nuevo sigue siendo insertar una fila en
 * {@code limit_dimensions}. La alternativa que evitaria el despliegue —un
 * identificador polimorfico con una columna de tipo— es la <em>Polymorphic
 * Association</em> que rechaza <cite>SQL Antipatterns</cite> (Karwin, cap. 8),
 * y aqui sostiene un cobro.
 */
public enum UsageBranch {

    /** Propietario dado de alta. Referencia a {@code owners}. */
    OWNER,

    /** Mascota dada de alta. Referencia a {@code animals}. */
    ANIMAL,

    /** Cita agendada. Referencia a {@code appointments}. */
    APPOINTMENT,

    /**
     * Documento electronico emitido. Referencia a {@code electronic_documents}.
     *
     * <p>
     * <strong>El codigo del eje es {@code INVOICE} y la columna es
     * {@code usage_electronic_document_id}</strong>: no coinciden, y es a proposito
     * —el eje se vende como «facturas» y la tabla guarda documentos electronicos,
     * que incluyen notas—. Si algun dia se renombra el eje en
     * {@code limit_dimensions}, hay que renombrarlo aqui <em>y</em> en el
     * {@code CHECK}, o la clave foranea compuesta deja de casar.
     */
    INVOICE;

    /**
     * El eje del catalogo traducido a rama, o un fallo en voz alta.
     *
     * <p>
     * <strong>Se niega ruidosamente en vez de devolver vacio</strong>: un eje de
     * existencias que llega hasta aqui no es un dato que falte, es una llamada que
     * no deberia haberse hecho, y tragarsela dejaria el hecho sin escribir sin que
     * nadie se enterara. El mensaje nombra el eje y explica la diferencia entre
     * acumular y contar, porque quien lo lea va a estar mirando por que «no se
     * registra el consumo» de algo que si se vendio.
     */
    public static UsageBranch ofDimensionCode(String limitDimensionCode) {
        if (limitDimensionCode == null || limitDimensionCode.isBlank()) {
            throw new IllegalArgumentException("limitDimensionCode is required");
        }
        for (UsageBranch branch : values()) {
            if (branch.name().equals(limitDimensionCode)) {
                return branch;
            }
        }
        throw new IllegalArgumentException("Limit dimension '" + limitDimensionCode
                + "' does not accumulate usage events: company_usage_events only accepts the four"
                + " countable axes (OWNER, ANIMAL, APPOINTMENT, INVOICE). Stock axes such as USER,"
                + " BRANCH, TERMINAL or STORAGE_GB are counted against their own table, not summed"
                + " fact by fact, and chk_cue_branch rejects them at the engine");
    }

    /** El codigo tal como viaja a {@code limit_dimension_code}. */
    public String code() {
        return name();
    }
}
