package com.vetsoftware.app.accountmapping.domain;

import java.time.LocalDate;

/**
 * Se intento cerrar la vigencia de un mapeo que ya estaba cerrado.
 *
 * <p>
 * <strong>La base no lo impide</strong>, y el motivo es exactamente el mismo
 * que en {@code withholding_rate_rules}: {@code current_mapping_marker} vale
 * {@code NULL} en cuanto {@code valid_to} deja de serlo, y una unicidad sobre
 * columna nula no restringe nada. El segundo cierre pasaria en silencio y
 * machacaria la fecha desde la que el mapeo dejo de aplicarse — que es lo que
 * decide contra que cuenta se asento cada factura de ese periodo.
 */
public class AccountMappingAlreadyClosedException extends RuntimeException {

    public AccountMappingAlreadyClosedException(Long id, LocalDate validTo) {
        super("Account mapping " + id + " is already closed since " + validTo);
    }
}
