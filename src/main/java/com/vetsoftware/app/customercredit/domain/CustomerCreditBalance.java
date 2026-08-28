package com.vetsoftware.app.customercredit.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La fila resumen del saldo a favor: una por empresa.
 *
 * <p>
 * <strong>NO ES LA VERDAD.</strong> La verdad es
 * {@code SUM(customer_credit_entries.amount)}, y esta fila es una
 * <em>proyeccion</em> que se puede tirar y rehacer. Un recuento periodico
 * afirma que las dos coinciden y {@link #getRecalculatedAt()} es su marca; si
 * divergen, manda el libro y esta fila se rehace.
 *
 * <p>
 * <strong>Existe por dos motivos, y ninguno es «para no sumar».</strong> El
 * primero: sumar filas no impide gastar credito que no existe, porque <em>una
 * suma es una lectura que no bloquea a nadie</em>. El segundo: «quien tiene
 * saldo aplicable» vuelve a ser un indice en vez de recorrer el historico
 * entero.
 *
 * <p>
 * <strong>Sin mutadores, y es deliberado.</strong> Esta clase no se lee, se
 * modifica en memoria y se guarda —que es exactamente la clase de defecto que
 * el libro de asientos vino a eliminar—. El saldo se mueve con un unico
 * {@code UPDATE} condicional que lleva la barandilla dentro de su propio
 * {@code WHERE}; ver
 * {@code CustomerCreditBalanceRepository#applyDelta(Long, BigDecimal, LocalDateTime)}.
 * Aqui solo se lee lo que esa instruccion dejo escrito.
 */
public class CustomerCreditBalance {

    private final Long id;
    private final Long companyId;
    private final BigDecimal balanceAmount;

    /** La caducidad mas proxima entre los lotes vivos. Vacia si ninguno caduca. */
    private final LocalDate nextExpiryOn;

    /** Marca del ultimo cuadre contra el libro. */
    private final LocalDateTime recalculatedAt;

    private final Long version;

    public CustomerCreditBalance(Long id, Long companyId, BigDecimal balanceAmount,
            LocalDate nextExpiryOn, LocalDateTime recalculatedAt, Long version) {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (balanceAmount == null)
            throw new IllegalArgumentException("balanceAmount is required");
        // Espejo de chk_ccb_not_negative. El motor es el cinturon encima del
        // tirante: aunque alguien escriba el UPDATE sin su condicion, rechaza el
        // negativo. Esta comprobacion caza ademas la fila leida de una base ya
        // corrompida antes de que su valor se propague a una respuesta.
        if (balanceAmount.signum() < 0)
            throw new IllegalArgumentException("balanceAmount cannot be negative");
        if (recalculatedAt == null)
            throw new IllegalArgumentException("recalculatedAt is required");
        this.id = id;
        this.companyId = companyId;
        this.balanceAmount = balanceAmount;
        this.nextExpiryOn = nextExpiryOn;
        this.recalculatedAt = recalculatedAt;
        this.version = version;
    }

    /**
     * Fila recien abierta para una empresa que aun no tenia saldo.
     *
     * <p>
     * <strong>Nace escribiendo su cero.</strong> La columna del importe no lleva
     * valor por defecto en el changeset a proposito: un importe con defecto cero
     * convierte un fallo de calculo en un dato plausible.
     */
    public static CustomerCreditBalance open(Long companyId, LocalDateTime at) {
        return new CustomerCreditBalance(null, companyId, BigDecimal.ZERO, null, at, null);
    }

    /** Si queda algo que aplicar. */
    public boolean hasCredit() {
        return balanceAmount.signum() > 0;
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public BigDecimal getBalanceAmount() {
        return balanceAmount;
    }

    public LocalDate getNextExpiryOn() {
        return nextExpiryOn;
    }

    public LocalDateTime getRecalculatedAt() {
        return recalculatedAt;
    }

    public Long getVersion() {
        return version;
    }
}
