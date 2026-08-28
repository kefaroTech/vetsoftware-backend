package com.vetsoftware.app.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Afirma <strong>qué</strong> restricción del motor paró una sentencia, por su
 * nombre.
 *
 * <h2>Por qué existe</h2>
 *
 * <p>
 * El patrón que sustituye es {@code assertThatThrownBy(...).isInstanceOf(
 * Exception.class)}: «lanzó <em>alguna</em> excepción». Sobre una tabla con
 * varias barandillas eso no prueba nada, porque casi todo escenario se puede
 * romper por más de un motivo. Si el andamio ya dejó sembrada una fila con la
 * misma clave única, la unicidad salta <em>antes</em> que la clave foránea
 * compuesta o el {@code CHECK} que el caso dice estar probando, y la prueba
 * sigue verde el día que esa comprobación se borre del esquema. Ese es
 * exactamente el defecto que dejó dos casos de este bloque pasando por el
 * motivo equivocado durante meses.
 *
 * <p>
 * Nombrar la restricción convierte el caso en lo que decía ser: si mañana un
 * cambio en los datos de siembra hace que salte otra, la prueba se pone roja en
 * vez de seguir pasando por el camino que no es.
 *
 * <h2>Por qué recorre la cadena de causas</h2>
 *
 * <p>
 * El nombre de la restricción lo escribe el driver de MySQL en el mensaje del
 * {@code SQLIntegrityConstraintViolationException} del fondo, y encima van
 * Hibernate y Spring envolviéndolo. {@code hasMessageContaining} solo mira el
 * mensaje de arriba, que muchas veces es {@code "could not execute statement"}
 * y nada más. Aquí se concatena la cadena entera.
 *
 * <p>
 * Los cuatro mensajes que produce MySQL 8.4 y que este método reconoce:
 * <ul>
 * <li>unicidad — {@code Duplicate entry '…' for key 'tabla.uq_lo_que_sea'}
 * <li>clave foránea —
 * {@code …fails (`db`.`tabla`, CONSTRAINT `fk_lo_que_sea`…)}
 * <li>comprobación — {@code Check constraint 'chk_lo_que_sea' is violated}
 * <li>columna obligatoria — {@code Column 'columna' cannot be null}, que no
 * lleva nombre de restricción: en ese caso se nombra la columna
 * </ul>
 */
public final class EngineConstraint {

    private EngineConstraint() {
    }

    /**
     * Ejecuta la sentencia esperando que el motor la rechace, y afirma que la
     * rechazó <em>esa</em> restricción.
     *
     * @param constraintName
     *            el nombre tal como lo declara la migración ({@code uq_…},
     *            {@code fk_…}, {@code chk_…}), o el nombre de la columna cuando lo
     *            que falla es un {@code NOT NULL}
     * @param statement
     *            la sentencia prohibida, con su {@code flush()} dentro: sin él la
     *            escritura se queda en la caché de Hibernate y no llega al motor
     */
    public static void assertViolates(String constraintName, ThrowingCallable statement) {
        assertThatThrownBy(statement).isInstanceOf(Exception.class)
                .satisfies(error -> assertThat(causeChain(error))
                        .as("la restricción del motor que paró la sentencia")
                        .contains(constraintName));
    }

    /**
     * Todos los mensajes de la cadena, de fuera adentro. La guarda contra el ciclo
     * no es teórica: una excepción que se pone a sí misma como causa cuelga el
     * recorrido, y ha pasado en envoltorios de drivers.
     */
    private static String causeChain(Throwable error) {
        StringBuilder mensajes = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            mensajes.append(current).append(System.lineSeparator());
            current = current.getCause() == current ? null : current.getCause();
        }
        return mensajes.toString();
    }
}
