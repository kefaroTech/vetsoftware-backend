package com.vetsoftware.app.accountingperiod.domain;

/**
 * El periodo ya estaba cerrado cuando se intento cerrarlo o declararlo.
 *
 * <p>
 * <strong>Es un conflicto y no un cuerpo mal formado</strong>: la peticion esta
 * bien escrita y lo que choca es el estado de la fila en este instante. El
 * escenario real es el proceso de cierre mensual disparado dos veces —a mano y
 * por el programador de tareas— sobre el mismo mes; quien lo ve necesita saber
 * que el trabajo ya estaba hecho, no corregir un campo. Mapea a 409.
 *
 * <p>
 * <strong>Y es la mitad de arriba de una defensa de dos capas.</strong> Debajo
 * esta {@code @Version}: si los dos cierres llegan en el mismo instante, los
 * dos leen {@code OPEN}, los dos pasan por aqui y el bloqueo optimista para al
 * segundo en el {@code UPDATE}. Esta excepcion cubre el caso comun —el que
 * llega tarde y lo sabe la lectura— y la version cubre el empate exacto.
 */
public class AccountingPeriodAlreadyClosedException extends RuntimeException {

    public AccountingPeriodAlreadyClosedException(Long id, AccountingPeriodStatus status) {
        super("Accounting period " + id + " is already closed with status " + status);
    }
}
