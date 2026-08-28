package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Conversion de las columnas {@code DATE} que devuelve una consulta nativa.
 *
 * <p>
 * <b>Existe porque el tipo que llega no esta garantizado.</b> Segun el driver y
 * la version de Hibernate, una columna {@code DATE} de una consulta nativa sin
 * mapeo llega como {@link java.sql.Date} o ya como {@link LocalDate}, y un
 * {@code cast} directo a uno de los dos funciona en local y revienta en el
 * contenedor —o al reves—. Aceptar las dos formas cuesta cuatro lineas y evita
 * un fallo que solo aparece en el entorno donde no estas mirando.
 */
final class SqlDates {

    private SqlDates() {
    }

    /** {@code null} cuando la columna es nula, que es un estado legitimo aqui. */
    static LocalDate toLocalDate(Object value) {
        if (value == null)
            return null;
        if (value instanceof LocalDate fecha)
            return fecha;
        if (value instanceof Date fecha)
            return fecha.toLocalDate();
        if (value instanceof java.util.Date fecha)
            return new Date(fecha.getTime()).toLocalDate();
        throw new IllegalStateException(
                "No se pudo leer la fecha de la consulta nativa: " + value.getClass().getName());
    }
}
