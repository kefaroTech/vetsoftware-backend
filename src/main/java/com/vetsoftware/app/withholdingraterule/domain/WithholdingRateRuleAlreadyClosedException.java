package com.vetsoftware.app.withholdingraterule.domain;

import java.time.LocalDate;

/**
 * Se intento cerrar una vigencia que ya estaba cerrada.
 *
 * <p>
 * Es un conflicto (409) y no una peticion mal formada: el cuerpo es valido y lo
 * que falla es el estado de la regla en este instante. Un 400 invitaria a
 * corregir el JSON cuando lo que hay que hacer es mirar que la regla ya tiene
 * fecha de fin.
 *
 * <p>
 * <strong>Por que el dominio lo mira antes que la base.</strong> Cerrar dos
 * veces no viola ninguna constraint —{@code current_rule_marker} vale
 * {@code NULL} en cuanto {@code valid_to} deja de serlo, y la unicidad sobre
 * una columna nula no restringe nada—, asi que el segundo cierre <b>pasaria en
 * silencio</b> y machacaria la fecha de fin del primero. Ese es exactamente el
 * dato que decide desde cuando deja de aplicarse una tarifa.
 */
public class WithholdingRateRuleAlreadyClosedException extends RuntimeException {

    public WithholdingRateRuleAlreadyClosedException(Long id, LocalDate validTo) {
        super("Withholding rate rule " + id + " is already closed since " + validTo);
    }
}
