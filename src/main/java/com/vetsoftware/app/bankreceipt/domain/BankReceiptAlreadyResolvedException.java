package com.vetsoftware.app.bankreceipt.domain;

/**
 * La entrada ya salio de la bandeja y no se puede volver a resolver.
 *
 * <p>
 * <strong>Es un conflicto y no un cuerpo mal formado</strong>: la peticion esta
 * bien escrita y lo que choca es el estado de la fila en este instante. Un
 * operario que atiende la bandeja del mes ve una lista que se le quedo vieja en
 * pantalla y pulsa «identificar» sobre algo que un compañero ya archivo hace un
 * minuto; lo que necesita es refrescar, no corregir un campo.
 *
 * <p>
 * <strong>Y es la mitad de arriba de una defensa de dos capas.</strong> Debajo
 * esta {@code @Version}: si los dos operarios pulsan en el mismo instante, los
 * dos leen {@code UNIDENTIFIED}, los dos pasan por aqui y el bloqueo optimista
 * para al segundo en el {@code UPDATE}. Esta excepcion cubre el caso comun —el
 * que llega tarde y lo sabe la lectura— y la version cubre el empate exacto.
 */
public class BankReceiptAlreadyResolvedException extends RuntimeException {

    public BankReceiptAlreadyResolvedException(Long id, BankReceiptStatus status) {
        super("Bank receipt " + id + " is already resolved with status " + status);
    }
}
