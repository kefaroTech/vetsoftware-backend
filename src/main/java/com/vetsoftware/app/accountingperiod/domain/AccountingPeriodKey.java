package com.vetsoftware.app.accountingperiod.domain;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * La clave de un mes contable: {@code yyyy-MM}, exactamente siete caracteres.
 *
 * <p>
 * <strong>Es un value object y no un {@code String} suelto por una razon que se
 * paga en produccion.</strong> {@code period_key} es el destino de la clave
 * foranea de {@code external_invoice_reconciliations.posting_period}: un
 * {@code 2026-3} o un {@code 03/2026} no lo corrige la base con un mensaje
 * util, lo rechaza {@code chk_accounting_periods_key} con un error de
 * comprobacion que no dice ni que columna ni que valor. Envolverlo aqui
 * convierte ese fallo en un mensaje que nombra el campo y el valor recibido.
 *
 * <p>
 * <strong>El orden lexicografico ES el orden cronologico, y de eso depende la
 * resolucion del periodo de imputacion.</strong> Con el ano de cuatro digitos y
 * el mes de dos con cero a la izquierda, {@code 2026-09} va antes que
 * {@code 2026-10} como cadena y como fecha. Por eso {@code findFirstOpenFrom}
 * puede pedirle al motor un {@code period_key >= ?} ordenado ascendente y
 * obtener «el primer periodo abierto de esta fecha en adelante» sin convertir
 * nada. Si alguien admitiera aqui un formato sin cero a la izquierda, esa
 * consulta empezaria a devolver el mes equivocado <em>sin fallar</em>:
 * {@code 2026-9} ordena despues de {@code 2026-10}.
 *
 * <p>
 * La comparacion la resuelve el motor bajo la colacion {@code ascii_bin} de la
 * columna —byte a byte— y sobre digitos ASCII eso coincide con
 * {@link String#compareTo}, que es lo que usa {@link #compareTo}. Las dos
 * mitades de la resolucion —la de Java y la del SQL— ordenan igual.
 */
public record AccountingPeriodKey(String value) implements Comparable<AccountingPeriodKey> {

    /**
     * Espejo <strong>literal</strong> de {@code chk_accounting_periods_key}. Se
     * copia el regex de la migracion tal cual —incluida la alternativa
     * {@code (0[1-9]|1[0-2])}, que es lo que descarta el mes 00 y el 13— para que
     * lo que el dominio acepta y lo que la base acepta no puedan divergir en una
     * revision.
     */
    private static final Pattern FORMAT = Pattern.compile("^[0-9]{4}-(0[1-9]|1[0-2])$");

    public AccountingPeriodKey {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("periodKey is required");
        if (!FORMAT.matcher(value).matches())
            throw new IllegalArgumentException("periodKey must have the form yyyy-MM: " + value);
    }

    public static AccountingPeriodKey of(String value) {
        return new AccountingPeriodKey(value);
    }

    /**
     * El mes al que pertenece una fecha. Es la unica traduccion fecha-a-clave del
     * sistema: tenerla aqui evita que cada consumidor invente su propio
     * {@code DateTimeFormatter} y que uno de ellos use {@code YYYY} —el ano de la
     * semana ISO— en vez de {@code yyyy}, que el 31 de diciembre devuelve el ano
     * siguiente y desvia un asiento entero de ejercicio.
     */
    public static AccountingPeriodKey from(LocalDate date) {
        if (date == null)
            throw new IllegalArgumentException("date is required");
        return new AccountingPeriodKey("%04d-%02d".formatted(date.getYear(), date.getMonthValue()));
    }

    @Override
    public int compareTo(AccountingPeriodKey other) {
        return value.compareTo(other.value);
    }

    /** Este mes va despues del otro en el calendario. */
    public boolean isAfter(AccountingPeriodKey other) {
        return compareTo(other) > 0;
    }

    /** Este mes va antes que el otro en el calendario. */
    public boolean isBefore(AccountingPeriodKey other) {
        return compareTo(other) < 0;
    }

    @Override
    public String toString() {
        return value;
    }
}
